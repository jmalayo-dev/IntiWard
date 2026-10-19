package pe.intiguard.inventario.dto;

import java.time.LocalDateTime;

/** cantidad lleva signo solo en AJUSTE; en los demás tipos siempre es positiva. */
public record MovimientoResponse(Long id, Long idProducto, String tipo, int cantidad, Long idVenta, String motivo,
                                 LocalDateTime creadoEn) {
}
