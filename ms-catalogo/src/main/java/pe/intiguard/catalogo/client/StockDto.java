package pe.intiguard.catalogo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Copia local de lo que devuelve ms-inventario. disponible ya descuenta lo reservado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockDto(Long idProducto, int disponible) {
}
