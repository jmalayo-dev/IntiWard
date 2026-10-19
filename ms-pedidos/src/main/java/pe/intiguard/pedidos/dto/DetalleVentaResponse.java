package pe.intiguard.pedidos.dto;

import java.math.BigDecimal;

/** Nombre y precio guardados al momento de la venta: si el catálogo cambia, el pedido no. */
public record DetalleVentaResponse(Long idProducto, String nombreProducto, BigDecimal precioUnitario, int cantidad,
                                   BigDecimal importe) {
}
