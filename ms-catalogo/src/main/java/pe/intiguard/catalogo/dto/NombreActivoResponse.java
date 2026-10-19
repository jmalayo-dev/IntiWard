package pe.intiguard.catalogo.dto;

/** Marca o categoría. productos = cantidad de productos activos (contador de los filtros y de la vista 5.4). */
public record NombreActivoResponse(Long id, String nombre, boolean activo, long productos) {
}
