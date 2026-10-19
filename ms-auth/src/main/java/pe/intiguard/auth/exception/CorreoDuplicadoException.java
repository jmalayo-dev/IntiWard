package pe.intiguard.auth.exception;

/** 409: el correo es único en db_auth.usuario. */
public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException() {
        super("El correo ya está registrado");
    }
}
