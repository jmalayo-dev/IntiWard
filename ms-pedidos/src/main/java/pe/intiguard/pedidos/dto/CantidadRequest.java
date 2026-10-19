package pe.intiguard.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** PUT /carrito/items/{idProducto}: cantidad absoluta. */
public record CantidadRequest(
        @NotNull(message = "es obligatoria") @Positive(message = "debe ser un entero mayor que 0") Integer cantidad) {
}
