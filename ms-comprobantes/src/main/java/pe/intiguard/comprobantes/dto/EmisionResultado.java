package pe.intiguard.comprobantes.dto;

/** nuevo = false cuando la venta ya tenía comprobante (idempotencia: se responde 200 en vez de 201). */
public record EmisionResultado(ComprobanteResponse comprobante, boolean nuevo) {
}
