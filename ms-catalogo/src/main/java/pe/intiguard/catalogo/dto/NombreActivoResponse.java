package pe.intiguard.catalogo.dto;

/**
 * Marca o categoría. productos = cantidad de productos activos (contador de los filtros y de la vista 5.4).
 * conStock = cuántos de esos productos tienen unidades disponibles (solo en GET /marcas; la web tacha las marcas con 0).
 * Es null si no aplica o si ms-inventario no respondió: en ese caso no se bloquea ninguna marca.
 */
public record NombreActivoResponse(Long id, String nombre, boolean activo, long productos, Long conStock) {
}
