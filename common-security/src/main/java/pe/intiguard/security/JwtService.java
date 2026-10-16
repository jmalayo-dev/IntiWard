package pe.intiguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Genera y valida tokens. ms-auth lo usa para emitir el token en el login;
 * los demás servicios lo usan para leer el usuario y el rol del claim.
 */
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_ID_USUARIO = "idUsuario";
    private static final int MINIMO_BYTES_CLAVE = 32;

    private final JwtProperties properties;

    /** Falla al arrancar si la clave no sirve: mejor eso que rechazar todos los tokens sin explicar por qué. */
    public JwtService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMO_BYTES_CLAVE) {
            throw new IllegalStateException("intiguard.jwt.secret (JWT_SECRET) falta o tiene menos de "
                    + MINIMO_BYTES_CLAVE + " caracteres. Revisa tu .env: una línea \"JWT_SECRET=\" vacía cuenta como definida.");
        }
        this.properties = properties;
    }

    /** Contrato C1: sub = correo, rol = ADMIN/CLIENTE, idUsuario = id en db_auth. */
    public String generarToken(Long idUsuario, String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_ID_USUARIO, idUsuario)
                .claim(CLAIM_ROL, rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(clave())
                .compact();
    }

    /** Lanza una JwtException si el token está vencido, mal firmado o mal formado. */
    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(clave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
