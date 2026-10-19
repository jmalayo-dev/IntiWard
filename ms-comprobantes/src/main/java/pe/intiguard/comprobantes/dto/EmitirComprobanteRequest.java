package pe.intiguard.comprobantes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /comprobantes (contrato C7, solo lo llama ms-pedidos).
 * Las reglas del documento según el tipo (DNI o RUC) se validan en ComprobanteService.
 */
public record EmitirComprobanteRequest(
        @NotNull(message = "es obligatorio") Long idVenta,
        @NotNull(message = "es obligatorio") Long idUsuario,
        @NotNull(message = "es obligatorio") @Pattern(regexp = "BOLETA|FACTURA", message = "debe ser BOLETA o FACTURA") String tipo,
        String docCliente,
        @Size(max = 150, message = "admite hasta 150 caracteres") String nombreRazonSocial,
        @NotNull(message = "es obligatorio") @DecimalMin(value = "0.00", message = "no puede ser negativo") BigDecimal subtotal,
        @NotNull(message = "es obligatorio") @DecimalMin(value = "0.00", message = "no puede ser negativo") BigDecimal igv,
        @NotNull(message = "es obligatorio") @DecimalMin(value = "0.01", message = "debe ser mayor que 0") BigDecimal total) {
}
