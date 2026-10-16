package pe.intiguard.comprobantes.mapper;

import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.entity.Comprobante;

public final class ComprobanteMapper {

    private ComprobanteMapper() {
    }

    public static ComprobanteResponse toResponse(Comprobante c) {
        return new ComprobanteResponse(c.getId(), c.getIdVenta(), c.getTipo().name(), c.getNumero(), c.getDocCliente(),
                c.getNombreRazonSocial(), c.getSubtotal(), c.getIgv(), c.getTotal(), c.getPdfUrl(), c.getEmitidoEn());
    }
}
