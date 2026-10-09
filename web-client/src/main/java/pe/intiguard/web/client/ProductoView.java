package pe.intiguard.web.client;

import java.math.BigDecimal;

public record ProductoView(Long id, String nombre, String descripcion, BigDecimal precio, String imagenUrl,
                           String marca, String categoria) {
}
