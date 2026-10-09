package pe.intiguard.pedidos.mapper;

import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.entity.Venta;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaResponse toResponse(Venta v) {
        return new VentaResponse(v.getId(), v.getIdUsuario(), v.getFecha(), v.getTotal(), v.getEstado().name(), v.getIdComprobante());
    }
}
