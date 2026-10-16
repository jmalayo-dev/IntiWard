package pe.intiguard.auth.dto;

import java.time.LocalDateTime;

/** Lo que sale por HTTP. No incluye la clave. */
public record UsuarioResponse(Long id, String nombres, String apellidos, String correo, String telefono,
                              String direccion, String fotoUrl, String rol, boolean activo, LocalDateTime creadoEn) {
}
