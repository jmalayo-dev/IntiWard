package pe.intiguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Genera y valida tokens. ms-auth lo usa para emitir el token en el login;
 * los demás servicios lo usan para leer el usuario y el rol del claim.
 */
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_ROL = "rol";

    private final JwtProperties properties;

    public String generarToken(String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
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
