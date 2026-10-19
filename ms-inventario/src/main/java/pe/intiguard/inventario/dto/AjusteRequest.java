package pe.intiguard.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** PUT /stock/{idProducto}: nuevo disponible absoluto tras un conteo físico. */
public record AjusteRequest(
        @NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo") Integer disponible,
        @NotBlank(message = "es obligatorio") @Size(max = 200, message = "admite hasta 200 caracteres") String motivo) {
}
