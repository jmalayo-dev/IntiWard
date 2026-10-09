package pe.intiguard.comprobantes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprobanteResponse(Long id, Long idVenta, String tipo, String numero, BigDecimal total, LocalDateTime fechaEmision) {
}
