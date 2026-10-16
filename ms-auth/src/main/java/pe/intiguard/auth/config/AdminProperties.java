package pe.intiguard.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del administrador inicial (intiguard.admin.*).
 * En local tienen valores de desarrollo; en la nube vienen de Parameter Store.
 */
@ConfigurationProperties(prefix = "intiguard.admin")
public record AdminProperties(String correo, String clave, String nombres, String apellidos) {
}
