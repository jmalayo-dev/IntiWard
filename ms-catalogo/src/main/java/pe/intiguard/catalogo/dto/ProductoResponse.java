package pe.intiguard.catalogo.dto;

import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, String descripcion, BigDecimal precio, String imagenUrl,
                               String marca, String categoria) {
}
