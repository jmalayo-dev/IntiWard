package pe.intiguard.pedidos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.intiguard.pedidos.client.CatalogoClient;
import pe.intiguard.pedidos.client.ComprobantesClient;
import pe.intiguard.pedidos.client.InventarioClient;
import pe.intiguard.pedidos.client.ProductoDto;
import pe.intiguard.pedidos.config.CircuitBreakerConfig;
import pe.intiguard.pedidos.dto.CheckoutRequest;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.entity.EstadoVenta;
import pe.intiguard.pedidos.entity.TipoComprobante;
import pe.intiguard.pedidos.entity.Venta;
import pe.intiguard.pedidos.exception.NegocioException;
import pe.intiguard.pedidos.mapper.VentaMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checkout como saga por compensación (README de ms-pedidos):
 * <pre>
 *  1. Leer el carrito            2. Precio vigente en ms-catalogo     3. Venta PENDIENTE
 *  4. Reservar en ms-inventario  5. Emitir en ms-comprobantes (circuit breaker, 2 s)
 *  6. Confirmar el stock         7. Venta CONFIRMADA y carrito vacío
 *  Si 4 falla: venta ANULADA y 409.  Si 5 falla o tarda: liberar el stock, venta ANULADA y 503.
 * </pre>
 * Nunca queda una venta a medias con el stock descontado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern RUC = Pattern.compile("^(10|15|17|20)[0-9]{9}$");
    private static final Pattern ID_PRODUCTO = Pattern.compile("idProducto:\\s*(\\d+)");

    private final CarritoService carritoService;
    private final VentaRegistro ventaRegistro;
    private final CatalogoClient catalogoClient;
    private final InventarioClient inventarioClient;
    private final ComprobantesClient comprobantesClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final ObjectMapper objectMapper;

    public VentaResponse checkout(Long idUsuario, String token, CheckoutRequest request) {
        TipoComprobante tipo = TipoComprobante.valueOf(request.tipoComprobante());
        String doc = request.docCliente() == null || request.docCliente().isBlank() ? null : request.docCliente().trim();
        validarDocumento(tipo, doc);

        // 1 y 2. Carrito con el precio vigente de cada producto
        List<VentaRegistro.Linea> lineas = lineasDelCarrito(idUsuario);

        // 3. Venta PENDIENTE: desde aquí existe el idVenta
        Venta venta = ventaRegistro.crearPendiente(idUsuario, tipo, lineas);
        Long idVenta = venta.getId();
        log.info("Checkout de la venta {} (usuario {}): {} productos, total {}", idVenta, idUsuario, lineas.size(), venta.getTotal());

        // 4. Reservar todo o nada
        reservar(token, venta, lineas);

        // 5. Emitir el comprobante, con circuit breaker
        ComprobantesClient.EmitirRequest emision = new ComprobantesClient.EmitirRequest(idVenta, idUsuario, tipo.name(), doc,
                request.nombreRazonSocial().trim(), venta.getSubtotal(), venta.getIgv(), venta.getTotal());
        boolean emitido = circuitBreakerFactory.create(CircuitBreakerConfig.COMPROBANTES).run(
                () -> comprobantesClient.emitir(token, emision) != null,
                error -> {
                    log.warn("Venta {}: no se pudo emitir el comprobante ({}). Se compensa", idVenta, error.toString());
                    return false;
                });
        if (!emitido) {
            liberarSinFallar(token, idVenta);
            ventaRegistro.cambiarEstado(idVenta, EstadoVenta.ANULADA);
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de comprobantes no está disponible. No se realizó ningún cobro; intenta de nuevo en unos minutos");
        }

        // 6. Confirmar el stock. El comprobante ya existe: la venta queda firme aunque esto falle
        confirmarConReintentos(token, idVenta);

        // 7. Venta CONFIRMADA y carrito vacío
        Venta confirmada = ventaRegistro.cambiarEstado(idVenta, EstadoVenta.CONFIRMADA);
        carritoService.vaciar(idUsuario);
        log.info("Venta {} CONFIRMADA", idVenta);
        return VentaMapper.toResponse(confirmada);
    }

    private List<VentaRegistro.Linea> lineasDelCarrito(Long idUsuario) {
        List<CarritoService.Linea> items = carritoService.lineas(idUsuario);
        if (items.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
        }
        List<VentaRegistro.Linea> lineas = new ArrayList<>();
        for (CarritoService.Linea item : items) {
            ProductoDto p;
            try {
                p = catalogoClient.obtenerProducto(item.idProducto());
            } catch (FeignException.NotFound ex) {
                p = null;
            } catch (RuntimeException ex) {
                log.warn("ms-catalogo no respondió en el checkout: {}", ex.getMessage());
                throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El catálogo no está disponible. No se realizó ningún cobro; intenta de nuevo en unos minutos");
            }
            if (p == null || !p.estaActivo()) {
                String nombre = p == null ? "el producto " + item.idProducto() : p.nombre();
                throw new NegocioException(HttpStatus.CONFLICT, "Ya no está a la venta " + nombre + ". Quítalo del carrito",
                        List.of("idProducto: " + item.idProducto()));
            }
            lineas.add(new VentaRegistro.Linea(p.id(), p.nombre(), p.precio(), item.cantidad()));
        }
        return lineas;
    }

    private void reservar(String token, Venta venta, List<VentaRegistro.Linea> lineas) {
        List<InventarioClient.Item> items = lineas.stream().map(l -> new InventarioClient.Item(l.idProducto(), l.cantidad())).toList();
        try {
            inventarioClient.reservar(token, new InventarioClient.ReservarRequest(venta.getId(), items));
        } catch (FeignException.Conflict ex) {
            ventaRegistro.cambiarEstado(venta.getId(), EstadoVenta.ANULADA);
            Long idProducto = productoQueFallo(ex);
            Map<Long, String> nombres = lineas.stream()
                    .collect(Collectors.toMap(VentaRegistro.Linea::idProducto, VentaRegistro.Linea::nombre));
            String nombre = nombres.getOrDefault(idProducto, "uno de los productos");
            log.info("Venta {} ANULADA: stock insuficiente para el producto {}", venta.getId(), idProducto);
            throw new NegocioException(HttpStatus.CONFLICT, "Stock insuficiente para " + nombre,
                    idProducto == null ? List.of() : List.of("idProducto: " + idProducto));
        } catch (RuntimeException ex) {
            log.warn("Venta {}: ms-inventario no respondió al reservar: {}", venta.getId(), ex.getMessage());
            // Por si la reserva sí llegó a hacerse antes del corte
            liberarSinFallar(token, venta.getId());
            ventaRegistro.cambiarEstado(venta.getId(), EstadoVenta.ANULADA);
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de inventario no está disponible. No se realizó ningún cobro; intenta de nuevo en unos minutos");
        }
    }

    /** Lee "idProducto: 7" de los detalles del 409 de ms-inventario. */
    private Long productoQueFallo(FeignException ex) {
        try {
            JsonNode cuerpo = objectMapper.readTree(ex.contentUTF8());
            for (JsonNode detalle : cuerpo.path("detalles")) {
                var m = ID_PRODUCTO.matcher(detalle.asText());
                if (m.find()) return Long.valueOf(m.group(1));
            }
        } catch (Exception ignorada) {
            // Sin cuerpo legible: se responde sin el id
        }
        return null;
    }

    private void liberarSinFallar(String token, Long idVenta) {
        try {
            inventarioClient.liberar(token, new InventarioClient.Venta(idVenta));
        } catch (RuntimeException ex) {
            // La tarea de vencimiento de ms-inventario la libera a los 15 minutos
            log.error("Venta {}: no se pudo liberar el stock; se liberará al vencer la reserva: {}", idVenta, ex.getMessage());
        }
    }

    private void confirmarConReintentos(String token, Long idVenta) {
        for (int intento = 1; intento <= 3; intento++) {
            try {
                inventarioClient.confirmar(token, new InventarioClient.Venta(idVenta));
                return;
            } catch (RuntimeException ex) {
                log.warn("Venta {}: intento {} de confirmar el stock falló: {}", idVenta, intento, ex.getMessage());
            }
        }
        log.error("Venta {}: el stock no se pudo confirmar; revisar la reserva en ms-inventario", idVenta);
    }

    private static void validarDocumento(TipoComprobante tipo, String doc) {
        if (tipo == TipoComprobante.FACTURA && (doc == null || !RUC.matcher(doc).matches())) {
            throw NegocioException.datosInvalidos(List.of("docCliente: el RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20"));
        }
        if (tipo == TipoComprobante.BOLETA && doc != null && !DNI.matcher(doc).matches()) {
            throw NegocioException.datosInvalidos(List.of("docCliente: el DNI debe tener 8 dígitos"));
        }
    }
}
