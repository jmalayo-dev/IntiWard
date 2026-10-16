package pe.intiguard.auth.exception;

/** 401 con mensaje genérico: no revela si falló el correo, la clave o si la cuenta está inactiva. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
