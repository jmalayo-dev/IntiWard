package pe.intiguard.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(Long id, Long idUsuario, LocalDateTime fechaVenta, String estado, String tipoComprobante,
                            BigDecimal subtotal, BigDecimal igv, BigDecimal total, List<DetalleVentaResponse> detalles) {
}
