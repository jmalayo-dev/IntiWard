package pe.intiguard.catalogo.dto;

import java.math.BigDecimal;
import java.util.List;

/** activo también sale para los inactivos (contrato C5): la web decide qué mostrar. */
public record ProductoResponse(Long id, String nombre, String descripcion, BigDecimal precio, String imagenUrl,
                               String marca, String categoria, boolean activo, Long idMarca, Long idCategoria,
                               List<EspecificacionDto> especificaciones) {
}
