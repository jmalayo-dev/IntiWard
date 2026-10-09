package pe.intiguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /auth/login. Las validaciones son las que pide el plan. */
public record LoginRequest(
        @NotBlank @Email String correo,
        @NotBlank @Size(min = 8) String password) {
}
