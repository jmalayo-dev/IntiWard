package pe.intiguard.catalogo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros de GET /productos. Todos opcionales y se combinan con Y.
 * categoria y cada marca aceptan el id o el nombre exacto.
 */
public record FiltroProductos(String categoria, List<String> marcas, String q, BigDecimal precioMin, BigDecimal precioMax,
                              boolean conStock, boolean incluirInactivos) {
}
