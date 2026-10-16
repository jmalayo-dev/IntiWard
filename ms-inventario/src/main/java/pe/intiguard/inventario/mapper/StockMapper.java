package pe.intiguard.inventario.mapper;

import pe.intiguard.inventario.dto.StockResponse;
import pe.intiguard.inventario.entity.Stock;

public final class StockMapper {

    private StockMapper() {
    }

    public static StockResponse toResponse(Stock s) {
        return new StockResponse(s.getIdProducto(), s.getDisponible(), s.getReservado());
    }
}
