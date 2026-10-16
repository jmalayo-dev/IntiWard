package pe.intiguard.auth.exception;

/** 423 Locked: 5 intentos fallidos seguidos bloquean la cuenta 15 minutos. */
public class CuentaBloqueadaException extends RuntimeException {

    public CuentaBloqueadaException() {
        super("Cuenta bloqueada temporalmente por intentos fallidos. Intente más tarde");
    }
}
