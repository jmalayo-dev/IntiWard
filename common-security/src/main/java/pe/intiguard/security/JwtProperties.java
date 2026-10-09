package pe.intiguard.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT. Se llena desde application.yml:
 * intiguard.jwt.secret (variable JWT_SECRET) e intiguard.jwt.expiration-minutes.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "intiguard.jwt")
public class JwtProperties {

    /** Clave HMAC de al menos 32 caracteres. En la nube sale de Parameter Store. */
    private String secret;

    /** El plan pide expiración corta: una hora. */
    private long expirationMinutes = 60;
}
