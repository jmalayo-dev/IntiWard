package pe.intiguard.catalogo.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de error para todas las respuestas del servicio. */
public record ApiError(LocalDateTime fecha, int status, String error, String mensaje, String ruta, List<String> detalles) {
}
