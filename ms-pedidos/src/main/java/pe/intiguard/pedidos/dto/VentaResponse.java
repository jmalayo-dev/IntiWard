package pe.intiguard.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponse(Long id, Long idUsuario, LocalDateTime fecha, BigDecimal total, String estado, Long idComprobante) {
}
