package pe.intiguard.pedidos.client;

import java.math.BigDecimal;

/** Copia local de lo que devuelve ms-catalogo. Los servicios no comparten clases de dominio. */
public record ProductoDto(Long id, String nombre, BigDecimal precio) {
}
