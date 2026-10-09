package pe.intiguard.auth.dto;

import java.time.LocalDateTime;

/** Lo que sale por HTTP. No incluye la contraseña. */
public record UsuarioResponse(Long id, String nombre, String correo, String rol, boolean activo, LocalDateTime fechaRegistro) {
}
