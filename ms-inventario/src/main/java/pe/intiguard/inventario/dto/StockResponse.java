package pe.intiguard.inventario.dto;

import java.time.LocalDateTime;

/** disponible ya descuenta lo reservado. actualizadoEn es null si el producto aún no tiene fila de stock. */
public record StockResponse(Long idProducto, int disponible, int reservado, LocalDateTime actualizadoEn) {

    /** Producto sin entradas: su stock es 0 (no es un error). */
    public static StockResponse vacio(Long idProducto) {
        return new StockResponse(idProducto, 0, 0, null);
    }
}
