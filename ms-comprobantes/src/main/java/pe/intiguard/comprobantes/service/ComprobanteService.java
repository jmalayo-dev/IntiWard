package pe.intiguard.comprobantes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.dto.EmisionResultado;
import pe.intiguard.comprobantes.dto.EmitirComprobanteRequest;
import pe.intiguard.comprobantes.dto.ReporteResponse;
import pe.intiguard.comprobantes.entity.Comprobante;
import pe.intiguard.comprobantes.entity.SerieCorrelativo;
import pe.intiguard.comprobantes.entity.TipoComprobante;
import pe.intiguard.comprobantes.exception.NegocioException;
import pe.intiguard.comprobantes.exception.RecursoNoEncontradoException;
import pe.intiguard.comprobantes.mapper.ComprobanteMapper;
import pe.intiguard.comprobantes.repository.ComprobanteRepository;
import pe.intiguard.comprobantes.repository.SerieCorrelativoRepository;
import pe.intiguard.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Emisión de boletas y facturas, consultas con control de dueño y reporte por rango.
 * El dueño se lee de comprobante.id_usuario: no hace falta preguntarle a ms-pedidos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComprobanteService {

    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern RUC = Pattern.compile("^(10|15|17|20)[0-9]{9}$");

    private final ComprobanteRepository comprobanteRepository;
    private final SerieCorrelativoRepository serieRepository;
    private final UsuarioActual usuarioActual;

    /**
     * Contrato C7. Idempotente por idVenta: si la venta ya tenía comprobante, lo devuelve sin numerar otro.
     * La serie se lee con SELECT ... FOR UPDATE: dos emisiones simultáneas no toman el mismo número.
     */
    @Transactional
    public EmisionResultado emitir(EmitirComprobanteRequest request) {
        var existente = comprobanteRepository.findByIdVenta(request.idVenta());
        if (existente.isPresent()) {
            return new EmisionResultado(ComprobanteMapper.toResponse(existente.get()), false);
        }

        TipoComprobante tipo = TipoComprobante.valueOf(request.tipo());
        String doc = vacioANulo(request.docCliente());
        String nombre = vacioANulo(request.nombreRazonSocial());
        validarDocumento(tipo, doc, nombre, request);

        SerieCorrelativo serie = serieRepository.findFirstByTipoOrderBySerieAsc(tipo)
                .orElseThrow(() -> new IllegalStateException("No hay serie configurada para " + tipo));
        int correlativo = serie.siguiente();

        Comprobante comprobante = comprobanteRepository.saveAndFlush(Comprobante.builder()
                .idVenta(request.idVenta())
                .idUsuario(request.idUsuario())
                .tipo(tipo)
                .serie(serie.getSerie())
                .correlativo(correlativo)
                .docCliente(doc)
                .nombreRazonSocial(nombre)
                .subtotal(request.subtotal())
                .igv(request.igv())
                .total(request.total())
                .build());
        log.info("Comprobante {} emitido para la venta {}", ComprobanteMapper.numero(comprobante), request.idVenta());
        return new EmisionResultado(ComprobanteMapper.toResponse(comprobante), true);
    }

    /** ADMIN: todos. CLIENTE: los suyos. Del más nuevo al más antiguo. */
    public List<ComprobanteResponse> listar() {
        List<Comprobante> lista = usuarioActual.esAdmin()
                ? comprobanteRepository.findAllByOrderByEmitidoEnDescIdDesc()
                : comprobanteRepository.findByIdUsuarioOrderByEmitidoEnDescIdDesc(usuarioActual.id());
        return lista.stream().map(ComprobanteMapper::toResponse).toList();
    }

    public ComprobanteResponse obtener(Long id) {
        return ComprobanteMapper.toResponse(buscarPropio(id));
    }

    public ComprobanteResponse obtenerPorVenta(Long idVenta) {
        Comprobante comprobante = comprobanteRepository.findByIdVenta(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay comprobante para la venta " + idVenta));
        verificarDueno(comprobante);
        return ComprobanteMapper.toResponse(comprobante);
    }

    /** El comprobante, solo si es de quien pide (o si pide un ADMIN). 403 si es de otro, 404 si no existe. */
    public Comprobante buscarPropio(Long id) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante " + id + " no existe"));
        verificarDueno(comprobante);
        return comprobante;
    }

    /** Fechas inclusivas sobre emitidoEn. Sin fechas: todo. */
    public ReporteResponse reporte(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw NegocioException.datosInvalidos(List.of("desde: no puede ser posterior a hasta"));
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime fin = hasta != null ? hasta.plusDays(1).atStartOfDay() : LocalDateTime.of(9999, 1, 1, 0, 0);
        List<Comprobante> lista = comprobanteRepository
                .findByEmitidoEnGreaterThanEqualAndEmitidoEnLessThanOrderByEmitidoEnAscIdAsc(inicio, fin);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Comprobante c : lista) {
            subtotal = subtotal.add(c.getSubtotal());
            igv = igv.add(c.getIgv());
            total = total.add(c.getTotal());
        }
        return new ReporteResponse(desde, hasta, lista.size(), subtotal, igv, total,
                lista.stream().map(ComprobanteMapper::toResponse).toList());
    }

    /** B001 + 1 -> "B001-00000001" */
    public static String formatearNumero(String serie, long correlativo) {
        return String.format("%s-%08d", serie, correlativo);
    }

    private void verificarDueno(Comprobante comprobante) {
        if (!usuarioActual.esAdmin() && !comprobante.getIdUsuario().equals(usuarioActual.id())) {
            throw new AccessDeniedException("El comprobante es de otro cliente");
        }
    }

    /** Las mismas reglas que la CHECK ck_comprobante_doc de la BD, pero con un 400 claro. */
    private static void validarDocumento(TipoComprobante tipo, String doc, String nombre, EmitirComprobanteRequest request) {
        List<String> detalles = new ArrayList<>();
        if (tipo == TipoComprobante.FACTURA) {
            if (doc == null || !RUC.matcher(doc).matches()) {
                detalles.add("docCliente: el RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20");
            }
            if (nombre == null) {
                detalles.add("nombreRazonSocial: es obligatorio en una factura");
            }
        } else if (doc != null && !DNI.matcher(doc).matches()) {
            detalles.add("docCliente: el DNI debe tener 8 dígitos");
        }
        if (request.subtotal().add(request.igv()).compareTo(request.total()) != 0) {
            detalles.add("total: debe ser igual a subtotal + igv");
        }
        if (!detalles.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
        }
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
