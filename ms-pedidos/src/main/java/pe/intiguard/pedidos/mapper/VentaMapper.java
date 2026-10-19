package pe.intiguard.pedidos.mapper;

import pe.intiguard.pedidos.dto.DetalleVentaResponse;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.entity.Venta;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaResponse toResponse(Venta v) {
        return new VentaResponse(v.getId(), v.getIdUsuario(), v.getFechaVenta(), v.getEstado().name(),
                v.getTipoComprobante().name(), v.getSubtotal(), v.getIgv(), v.getTotal(),
                v.getDetalles().stream().map(d -> new DetalleVentaResponse(d.getIdProducto(), d.getNombreProducto(),
                        d.getPrecioUnitario(), d.getCantidad(), d.getImporte())).toList());
    }
}
