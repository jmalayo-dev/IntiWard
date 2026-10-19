package pe.intiguard.auth.dto;

import java.time.LocalDateTime;

/**
 * Lo que sale por HTTP. No incluye la clave.
 * bloqueadoHasta es null si la cuenta no está bloqueada o si el bloqueo ya venció.
 */
public record UsuarioResponse(Long id, String nombres, String apellidos, String correo, String telefono,
                              String direccion, String fotoUrl, String rol, boolean activo, LocalDateTime creadoEn,
                              LocalDateTime bloqueadoHasta, LocalDateTime ultimoAcceso) {
}
