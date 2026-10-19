package pe.intiguard.pedidos.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Error de negocio con su código HTTP y, si hace falta, un detalle por campo ("campo: mensaje"). */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> detalles;

    public NegocioException(HttpStatus status, String mensaje) {
        this(status, mensaje, List.of());
    }

    public NegocioException(HttpStatus status, String mensaje, List<String> detalles) {
        super(mensaje);
        this.status = status;
        this.detalles = List.copyOf(detalles);
    }

    /** 400 "Datos inválidos" con los detalles por campo. */
    public static NegocioException datosInvalidos(List<String> detalles) {
        return new NegocioException(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
