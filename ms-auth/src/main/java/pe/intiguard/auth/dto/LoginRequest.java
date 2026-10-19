package pe.intiguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /auth/login. Las validaciones son las que pide el plan. */
public record LoginRequest(
        @NotBlank(message = "es obligatorio") @Email(message = "no tiene formato de correo") String correo,
        @NotBlank(message = "es obligatoria") @Size(min = 8, message = "debe tener al menos 8 caracteres") String password) {
}
