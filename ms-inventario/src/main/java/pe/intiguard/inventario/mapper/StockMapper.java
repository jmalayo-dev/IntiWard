package pe.intiguard.inventario.mapper;

import pe.intiguard.inventario.dto.MovimientoResponse;
import pe.intiguard.inventario.dto.StockResponse;
import pe.intiguard.inventario.entity.MovimientoStock;
import pe.intiguard.inventario.entity.Stock;

public final class StockMapper {

    private StockMapper() {
    }

    public static StockResponse toResponse(Stock s) {
        return new StockResponse(s.getIdProducto(), s.getDisponible(), s.getReservado(), s.getActualizadoEn());
    }

    public static MovimientoResponse toResponse(MovimientoStock m) {
        return new MovimientoResponse(m.getId(), m.getIdProducto(), m.getTipo().name(), m.getCantidad(), m.getIdVenta(),
                m.getMotivo(), m.getCreadoEn());
    }
}
