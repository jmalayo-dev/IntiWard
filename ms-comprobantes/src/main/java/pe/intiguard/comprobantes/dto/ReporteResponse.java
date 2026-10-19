package pe.intiguard.comprobantes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reporte de ventas de un período (GET /comprobantes/reporte). desde y hasta son null si no se filtraron. */
public record ReporteResponse(LocalDate desde, LocalDate hasta, int cantidad, BigDecimal subtotal, BigDecimal igv,
                              BigDecimal total, List<ComprobanteResponse> comprobantes) {
}
