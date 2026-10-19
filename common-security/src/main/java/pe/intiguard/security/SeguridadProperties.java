package pe.intiguard.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de acceso de cada servicio (intiguard.security.*):
 * <pre>
 * intiguard:
 *   security:
 *     exigir-token: ${EXIGIR_TOKEN:false}
 *     rutas-publicas:
 *       - GET /productos/**
 *       - POST /auth/login
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "intiguard.security")
public class SeguridadProperties {

    /**
     * false: el token se valida si llega, pero no es obligatorio (mientras no exista el login).
     * true: todo lo que no sea ruta pública exige un token válido (401 si falta).
     */
    private boolean exigirToken = false;

    /** "METODO /ruta/**" o "/ruta/**" (cualquier método). /actuator/health y /error siempre son públicas. */
    private List<String> rutasPublicas = new ArrayList<>();

    /** Rutas que solo llama otro servicio ("METODO /ruta"). Con clave-interna, exigen la cabecera X-Clave-Interna. */
    private List<String> rutasInternas = new ArrayList<>();

    /** Clave compartida entre servicios (CLAVE_INTERNA). Vacía = sin control (modo local). */
    private String claveInterna;
}
