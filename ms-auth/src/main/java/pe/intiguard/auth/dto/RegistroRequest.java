package pe.intiguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /auth/registro y de POST /usuarios.
 * rol solo lo usa POST /usuarios (ADMIN); en el registro público se ignora y siempre es CLIENTE.
 */
public record RegistroRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 80, message = "admite hasta 80 caracteres") String nombres,
        @NotBlank(message = "es obligatorio") @Size(max = 80, message = "admite hasta 80 caracteres") String apellidos,
        @NotBlank(message = "es obligatorio") @Email(message = "no tiene formato de correo")
        @Size(max = 120, message = "admite hasta 120 caracteres") String correo,
        @Size(max = 15, message = "admite hasta 15 caracteres") String telefono,
        @Size(max = 200, message = "admite hasta 200 caracteres") String direccion,
        @NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        String rol) {
}
