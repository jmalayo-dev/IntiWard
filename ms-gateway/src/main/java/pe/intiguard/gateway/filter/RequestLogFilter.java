package pe.intiguard.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Registra cada petición que entra al gateway.
 *
 * PENDIENTE (integrante 1, semana 3): crear JwtGatewayFilter al lado de este:
 *  - Rutas públicas: /auth/**, GET /productos/**, /actuator/health.
 *  - En el resto, validar "Authorization: Bearer ..." con la misma clave JWT_SECRET
 *    y responder 401 si falla.
 *  - Propagar el usuario y el rol a los servicios en las cabeceras X-User y X-Rol.
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("{} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
