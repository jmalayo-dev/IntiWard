package pe.intiguard.pedidos.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/** Manejo global de errores: ninguna excepción sale al cliente sin pasar por aquí. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Datos inválidos", req, detalles);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", req, List.of());
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, HttpServletRequest req, List<String> detalles) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
