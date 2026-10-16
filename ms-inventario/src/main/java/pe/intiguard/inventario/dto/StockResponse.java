package pe.intiguard.inventario.dto;

public record StockResponse(Long idProducto, int disponible, int reservado) {
}
