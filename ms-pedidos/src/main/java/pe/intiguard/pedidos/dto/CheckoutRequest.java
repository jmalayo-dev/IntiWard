package pe.intiguard.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /pedidos/checkout. Boleta: DNI opcional. Factura: RUC obligatorio (se valida en CheckoutService). */
public record CheckoutRequest(
        @NotNull(message = "debe ser BOLETA o FACTURA") @Pattern(regexp = "BOLETA|FACTURA", message = "debe ser BOLETA o FACTURA")
        String tipoComprobante,
        String docCliente,
        @NotBlank(message = "es obligatorio") @Size(max = 150, message = "admite hasta 150 caracteres") String nombreRazonSocial) {
}
