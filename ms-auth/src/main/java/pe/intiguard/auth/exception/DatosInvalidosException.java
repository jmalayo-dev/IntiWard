package pe.intiguard.auth.exception;

import java.util.List;

/** 400 con un detalle por campo ("campo: mensaje"), igual que las validaciones de @Valid. */
public class DatosInvalidosException extends RuntimeException {

    private final List<String> detalles;

    public DatosInvalidosException(List<String> detalles) {
        super("Datos inválidos");
        this.detalles = List.copyOf(detalles);
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
