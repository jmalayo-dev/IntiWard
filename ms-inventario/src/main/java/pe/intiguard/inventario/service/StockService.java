package pe.intiguard.inventario.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.inventario.client.CatalogoClient;
import pe.intiguard.inventario.dto.*;
import pe.intiguard.inventario.entity.*;
import pe.intiguard.inventario.exception.NegocioException;
import pe.intiguard.inventario.exception.RecursoNoEncontradoException;
import pe.intiguard.inventario.mapper.StockMapper;
import pe.intiguard.inventario.repository.MovimientoStockRepository;
import pe.intiguard.inventario.repository.ReservaRepository;
import pe.intiguard.inventario.repository.StockRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock por producto y su historial. Cada cambio deja un MovimientoStock (historial auditable).
 * Reglas del checkout (contrato C6): reservar es todo o nada; confirmar y liberar son idempotentes.
 * "disponible" ya descuenta lo reservado: al reservar baja disponible y sube reservado.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ReservaRepository reservaRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final CatalogoClient catalogoClient;
    private final int minutosReserva;

    public StockService(StockRepository stockRepository, ReservaRepository reservaRepository,
                        MovimientoStockRepository movimientoRepository, CatalogoClient catalogoClient,
                        @Value("${intiguard.reserva.minutos:15}") int minutosReserva) {
        this.stockRepository = stockRepository;
        this.reservaRepository = reservaRepository;
        this.movimientoRepository = movimientoRepository;
        this.catalogoClient = catalogoClient;
        this.minutosReserva = minutosReserva;
    }

    // ---------- Consultas ----------

    /** Sin fila de stock: 200 con todo en 0, no 404. */
    public StockResponse obtener(Long idProducto) {
        return stockRepository.findById(idProducto).map(StockMapper::toResponse).orElse(StockResponse.vacio(idProducto));
    }

    /** Con ids: una entrada por id pedido (en 0 si no tiene fila), en el mismo orden. Sin ids: todas las filas. */
    public List<StockResponse> listar(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return stockRepository.findAllByOrderByIdProductoAsc().stream().map(StockMapper::toResponse).toList();
        }
        Map<Long, Stock> filas = stockRepository.findByIdProductoIn(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(Stock::getIdProducto, Function.identity()));
        return ids.stream().distinct()
                .map(id -> filas.containsKey(id) ? StockMapper.toResponse(filas.get(id)) : StockResponse.vacio(id))
                .toList();
    }

    public List<MovimientoResponse> movimientos(Long idProducto) {
        List<MovimientoStock> lista = idProducto == null
                ? movimientoRepository.findAllByOrderByCreadoEnDescIdDesc()
                : movimientoRepository.findByIdProductoOrderByCreadoEnDescIdDesc(idProducto);
        return lista.stream().map(StockMapper::toResponse).toList();
    }

    // ---------- Panel (ADMIN) ----------

    /** Suma a disponible. Si el producto aún no tenía fila, la crea (validando en catálogo que exista). */
    @Transactional
    public StockResponse registrarEntrada(EntradaRequest request) {
        Stock stock = filaParaMover(request.idProducto());
        stock.setDisponible(stock.getDisponible() + request.cantidad());
        registrar(stock.getIdProducto(), TipoMovimiento.ENTRADA, request.cantidad(), null, textoONulo(request.motivo()));
        return StockMapper.toResponse(stockRepository.saveAndFlush(stock));
    }

    /** Fija el disponible tras un conteo y registra un AJUSTE con la diferencia con signo (si la hay). */
    @Transactional
    public StockResponse ajustar(Long idProducto, AjusteRequest request) {
        Stock stock = filaParaMover(idProducto);
        int diferencia = request.disponible() - stock.getDisponible();
        stock.setDisponible(request.disponible());
        // La base rechaza un movimiento con cantidad 0 (ck_movimiento_cantidad)
        if (diferencia != 0) {
            registrar(idProducto, TipoMovimiento.AJUSTE, diferencia, null, request.motivo().trim());
        }
        return StockMapper.toResponse(stockRepository.saveAndFlush(stock));
    }

    // ---------- Checkout (internos, contrato C6) ----------

    /**
     * Todo o nada: bloquea las filas, comprueba que todo alcance y recién entonces mueve el stock.
     * Si la venta ya tenía reservas, responde las mismas sin reservar dos veces.
     */
    @Transactional
    public ReservaResponse reservar(ReservarRequest request) {
        List<Reserva> previas = reservaRepository.findByIdVenta(request.idVenta());
        if (!previas.isEmpty()) {
            return new ReservaResponse(request.idVenta(), previas.get(0).getExpiraEn());
        }

        // Un producto repetido en el pedido se suma
        Map<Long, Integer> pedido = new TreeMap<>();
        for (ReservarRequest.Item item : request.items()) {
            pedido.merge(item.idProducto(), item.cantidad(), Integer::sum);
        }
        Map<Long, Stock> filas = stockRepository.bloquear(pedido.keySet()).stream()
                .collect(Collectors.toMap(Stock::getIdProducto, Function.identity()));

        for (var linea : pedido.entrySet()) {
            Stock stock = filas.get(linea.getKey());
            if (stock == null || stock.getDisponible() < linea.getValue()) {
                throw new NegocioException(HttpStatus.CONFLICT,
                        "Stock insuficiente para el producto " + linea.getKey(),
                        List.of("idProducto: " + linea.getKey()));
            }
        }

        LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(minutosReserva).withNano(0);
        for (var linea : pedido.entrySet()) {
            Stock stock = filas.get(linea.getKey());
            stock.setDisponible(stock.getDisponible() - linea.getValue());
            stock.setReservado(stock.getReservado() + linea.getValue());
            reservaRepository.save(Reserva.builder().idVenta(request.idVenta()).idProducto(linea.getKey())
                    .cantidad(linea.getValue()).expiraEn(expiraEn).build());
            registrar(linea.getKey(), TipoMovimiento.RESERVA, linea.getValue(), request.idVenta(), null);
        }
        log.info("Venta {}: reservados {} productos hasta {}", request.idVenta(), pedido.size(), expiraEn);
        return new ReservaResponse(request.idVenta(), expiraEn);
    }

    /** Las unidades reservadas salen vendidas: baja reservado (disponible ya bajó al reservar). */
    @Transactional
    public void confirmar(Long idVenta) {
        cerrarReservas(idVenta, EstadoReserva.CONFIRMADA);
    }

    /** Devuelve lo reservado a disponible. Sin reservas activas, no hace nada. */
    @Transactional
    public void liberar(Long idVenta) {
        cerrarReservas(idVenta, EstadoReserva.LIBERADA);
    }

    /** CU09: libera las reservas que pasaron su expiraEn. La llama TareaVencimientoReservas. */
    @Transactional
    public int liberarVencidas() {
        Set<Long> ventas = reservaRepository.findByEstadoAndExpiraEnBefore(EstadoReserva.ACTIVA, LocalDateTime.now())
                .stream().map(Reserva::getIdVenta).collect(Collectors.toCollection(TreeSet::new));
        ventas.forEach(idVenta -> cerrarReservas(idVenta, EstadoReserva.LIBERADA));
        return ventas.size();
    }

    private void cerrarReservas(Long idVenta, EstadoReserva nuevoEstado) {
        List<Reserva> activas = reservaRepository.findByIdVentaAndEstado(idVenta, EstadoReserva.ACTIVA);
        if (activas.isEmpty()) {
            return;
        }
        Map<Long, Stock> filas = stockRepository.bloquear(activas.stream().map(Reserva::getIdProducto).toList()).stream()
                .collect(Collectors.toMap(Stock::getIdProducto, Function.identity()));
        TipoMovimiento tipo = nuevoEstado == EstadoReserva.CONFIRMADA ? TipoMovimiento.CONFIRMACION : TipoMovimiento.LIBERACION;
        for (Reserva reserva : activas) {
            Stock stock = filas.get(reserva.getIdProducto());
            stock.setReservado(stock.getReservado() - reserva.getCantidad());
            if (nuevoEstado == EstadoReserva.LIBERADA) {
                stock.setDisponible(stock.getDisponible() + reserva.getCantidad());
            }
            reserva.setEstado(nuevoEstado);
            registrar(reserva.getIdProducto(), tipo, reserva.getCantidad(), idVenta, null);
        }
        log.info("Venta {}: {} reservas {}", idVenta, activas.size(), nuevoEstado);
    }

    // ---------- Apoyo ----------

    /** Fila bloqueada del producto; si no existe, la crea en 0 después de comprobar en catálogo que el producto exista. */
    private Stock filaParaMover(Long idProducto) {
        return stockRepository.bloquearUno(idProducto).orElseGet(() -> {
            validarEnCatalogo(idProducto);
            // Se guarda ya: el movimiento que viene después tiene FK hacia stock
            return stockRepository.saveAndFlush(Stock.builder().idProducto(idProducto).build());
        });
    }

    private void validarEnCatalogo(Long idProducto) {
        try {
            catalogoClient.obtenerProducto(idProducto);
        } catch (FeignException.NotFound ex) {
            throw new RecursoNoEncontradoException("Producto " + idProducto + " no existe");
        } catch (RuntimeException ex) {
            log.warn("No se pudo validar el producto {} en ms-catalogo: {}", idProducto, ex.getMessage());
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar el producto en el catálogo. Intenta de nuevo en unos segundos");
        }
    }

    private void registrar(Long idProducto, TipoMovimiento tipo, int cantidad, Long idVenta, String motivo) {
        movimientoRepository.save(MovimientoStock.builder().idProducto(idProducto).tipo(tipo).cantidad(cantidad)
                .idVenta(idVenta).motivo(motivo).build());
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
