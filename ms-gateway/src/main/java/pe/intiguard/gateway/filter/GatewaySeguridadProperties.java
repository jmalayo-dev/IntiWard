package pe.intiguard.gateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * intiguard.jwt.secret y intiguard.security.* del application.yml del gateway.
 * Reglas: "METODO /ruta/**" o "/ruta/**" (cualquier método).
 */
@ConfigurationProperties(prefix = "intiguard")
public record GatewaySeguridadProperties(Jwt jwt, Security security) {

    public record Jwt(String secret) {
    }

    public record Security(boolean exigirToken, List<String> rutasPublicas, List<String> rutasInternas) {

        public Security {
            rutasPublicas = rutasPublicas == null ? List.of() : rutasPublicas;
            rutasInternas = rutasInternas == null ? List.of() : rutasInternas;
        }
    }
}
