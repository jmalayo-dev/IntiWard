package pe.intiguard.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtGatewayFilterTest {

    private static final String CLAVE = "clave-de-prueba-de-al-menos-32-caracteres!!";

    private final JwtGatewayFilter conTokenObligatorio = filtro(true);
    private final JwtGatewayFilter conTokenOpcional = filtro(false);

    private static JwtGatewayFilter filtro(boolean exigirToken) {
        return new JwtGatewayFilter(new GatewaySeguridadProperties(
                new GatewaySeguridadProperties.Jwt(CLAVE),
                new GatewaySeguridadProperties.Security(exigirToken,
                        List.of("POST /auth/login", "GET /productos/**", "GET /stock/*"),
                        List.of("POST /stock/reservar", "POST /comprobantes"))));
    }

    private static String token(String clave, Instant vence) {
        return Jwts.builder().subject("ana@intiguard.pe").claim("rol", "CLIENTE").claim("idUsuario", 7)
                .expiration(Date.from(vence))
                .signWith(Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8))).compact();
    }

    /** Ejecuta el filtro y devuelve el status de la respuesta, o null si dejó pasar la petición. */
    private HttpStatus ejecutar(JwtGatewayFilter filtro, MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean paso = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            paso.set(true);
            return Mono.empty();
        };
        filtro.filter(exchange, chain).block();
        return paso.get() ? null : HttpStatus.valueOf(exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rutaInternaSiempreResponde403AunqueTraigaUnTokenValido() {
        var request = MockServerHttpRequest.method(HttpMethod.POST, "/stock/reservar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(CLAVE, Instant.now().plusSeconds(600)));
        assertThat(ejecutar(conTokenOpcional, request.build())).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.post("/comprobantes").build())).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elPreflightCorsPasaSinToken() {
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.options("/pedidos").build())).isNull();
    }

    @Test
    void rutaPublicaPasaSinToken() {
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.get("/productos/3").build())).isNull();
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.get("/stock/3").build())).isNull();
    }

    @Test
    void elMetodoCuenta() {
        // GET /comprobantes no es interna (solo POST): exige token como cualquier otra
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.get("/comprobantes").build())).isEqualTo(HttpStatus.UNAUTHORIZED);
        // La regla interna es POST /stock/reservar; un GET a esa ruta cae en la pública GET /stock/*
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.get("/stock/reservar").build())).isNull();
    }

    @Test
    void sinTokenResponde401SoloSiSeExige() {
        assertThat(ejecutar(conTokenObligatorio, MockServerHttpRequest.get("/pedidos").build())).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ejecutar(conTokenOpcional, MockServerHttpRequest.get("/pedidos").build())).isNull();
    }

    @Test
    void tokenValidoPasa() {
        var request = MockServerHttpRequest.get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(CLAVE, Instant.now().plusSeconds(600))).build();
        assertThat(ejecutar(conTokenObligatorio, request)).isNull();
    }

    @Test
    void tokenVencidoOFirmadoConOtraClaveResponde401AunqueNoSeExija() {
        var vencido = MockServerHttpRequest.get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(CLAVE, Instant.now().minusSeconds(60))).build();
        var otraClave = MockServerHttpRequest.get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("otra-clave-distinta-de-al-menos-32-caracteres", Instant.now().plusSeconds(600))).build();
        assertThat(ejecutar(conTokenOpcional, vencido)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ejecutar(conTokenOpcional, otraClave)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
