package pe.intiguard.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** POST /carrito/items. Si el producto ya estaba, se suma la cantidad (por defecto 1). */
public record ItemCarritoRequest(
        @NotNull(message = "es obligatorio") Long idProducto,
        @Positive(message = "debe ser un entero mayor que 0") Integer cantidad) {
}
