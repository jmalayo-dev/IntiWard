package pe.intiguard.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Contrato C6: POST /stock/reservar. Todo o nada. */
public record ReservarRequest(
        @NotNull(message = "es obligatorio") Long idVenta,
        @NotEmpty(message = "debe tener al menos un producto") List<@Valid Item> items) {

    public record Item(
            @NotNull(message = "es obligatorio") Long idProducto,
            @NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que 0") Integer cantidad) {
    }
}
