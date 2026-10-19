package pe.intiguard.inventario.dto;

import jakarta.validation.constraints.NotNull;

/** Contrato C6: POST /stock/confirmar y /stock/liberar. Las cantidades ya están en las reservas. */
public record VentaRequest(@NotNull(message = "es obligatorio") Long idVenta) {
}
