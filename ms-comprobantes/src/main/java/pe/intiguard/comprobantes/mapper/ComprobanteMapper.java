package pe.intiguard.comprobantes.mapper;

import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.entity.Comprobante;
import pe.intiguard.comprobantes.service.ComprobanteService;

public final class ComprobanteMapper {

    private ComprobanteMapper() {
    }

    /** idUsuario no sale: es un dato interno para el control de dueño. */
    public static ComprobanteResponse toResponse(Comprobante c) {
        return new ComprobanteResponse(c.getId(), c.getIdVenta(), c.getTipo().name(), numero(c), c.getDocCliente(),
                c.getNombreRazonSocial(), c.getSubtotal(), c.getIgv(), c.getTotal(), pdfUrl(c), c.getEmitidoEn());
    }

    /** MySQL arma el número (columna GENERATED); si todavía no se leyó, se calcula igual. */
    public static String numero(Comprobante c) {
        return c.getNumero() != null ? c.getNumero() : ComprobanteService.formatearNumero(c.getSerie(), c.getCorrelativo());
    }

    private static String pdfUrl(Comprobante c) {
        return c.getPdfUrl() != null ? c.getPdfUrl() : "/comprobantes/" + c.getId() + "/pdf";
    }
}
