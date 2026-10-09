package pe.intiguard.inventario.dto;

public record StockResponse(Long idProducto, int cantidad, int reservada, int disponible) {
}
