package pe.intiguard.gateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato C4: seguridad en la puerta de entrada. En este orden:
 * <ol>
 *   <li>Ruta interna (reservar, confirmar y liberar stock, emitir comprobante): 403 siempre.</li>
 *   <li>Ruta pública: pasa sin mirar el token.</li>
 *   <li>Token presente: se valida firma y expiración; si falla, 401.</li>
 *   <li>Sin token: 401 si exigir-token=true; si no, pasa (mientras no exista el login).</li>
 * </ol>
 * El token sigue viajando en Authorization: cada servicio lo vuelve a validar con common-security.
 */
@Slf4j
@Component
@EnableConfigurationProperties(GatewaySeguridadProperties.class)
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String PREFIJO = "Bearer ";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final GatewaySeguridadProperties.Security reglas;
    private final JwtParser parser;

    public JwtGatewayFilter(GatewaySeguridadProperties properties) {
        this.reglas = properties.security();
        this.parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (coincide(reglas.rutasInternas(), request)) {
            log.warn("Ruta interna bloqueada: {} {}", request.getMethod(), request.getURI().getPath());
            return responder(exchange, HttpStatus.FORBIDDEN, "Ruta de uso interno entre servicios");
        }
        if (coincide(reglas.rutasPublicas(), request)) {
            return chain.filter(exchange);
        }
        String cabecera = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return reglas.exigirToken()
                    ? responder(exchange, HttpStatus.UNAUTHORIZED, "Se requiere un token válido")
                    : chain.filter(exchange);
        }
        try {
            parser.parseSignedClaims(cabecera.substring(PREFIJO.length()));
        } catch (JwtException | IllegalArgumentException ex) {
            return responder(exchange, HttpStatus.UNAUTHORIZED, "Token inválido o vencido");
        }
        return chain.filter(exchange);
    }

    /** Después de RequestLogFilter, para que también queden en el log las peticiones rechazadas. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private boolean coincide(List<String> reglasRuta, ServerHttpRequest request) {
        String ruta = request.getURI().getPath();
        for (String regla : reglasRuta) {
            String[] partes = regla.trim().split("\\s+", 2);
            if (partes.length == 2) {
                if (request.getMethod().name().equalsIgnoreCase(partes[0]) && matcher.match(partes[1], ruta)) {
                    return true;
                }
            } else if (matcher.match(partes[0], ruta)) {
                return true;
            }
        }
        return false;
    }

    /** Mismo formato que el ApiError de los servicios. */
    private Mono<Void> responder(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = """
                {"fecha":"%s","status":%d,"error":"%s","mensaje":"%s","ruta":"%s","detalles":[]}"""
                .formatted(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje,
                        exchange.getRequest().getURI().getPath().replace("\"", ""));
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
