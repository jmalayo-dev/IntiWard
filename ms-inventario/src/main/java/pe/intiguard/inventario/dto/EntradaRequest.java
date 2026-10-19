package pe.intiguard.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** POST /stock/entrada: mercadería que llegó del proveedor. */
public record EntradaRequest(
        @NotNull(message = "es obligatorio") Long idProducto,
        @NotNull(message = "es obligatoria") @Positive(message = "debe ser un entero mayor que 0") Integer cantidad,
        @Size(max = 200, message = "admite hasta 200 caracteres") String motivo) {
}
