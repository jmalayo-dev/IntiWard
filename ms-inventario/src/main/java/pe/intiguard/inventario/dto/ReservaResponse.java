package pe.intiguard.inventario.dto;

import java.time.LocalDateTime;

public record ReservaResponse(Long idVenta, LocalDateTime expiraEn) {
}
