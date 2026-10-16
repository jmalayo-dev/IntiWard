package pe.intiguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Contrato C2. Lee "Authorization: Bearer <token>", lo valida con JwtService y deja
 * el UsuarioAutenticado con su rol (ROLE_ADMIN / ROLE_CLIENTE) en el SecurityContext.
 * Sin cabecera, sigue como anónimo y deciden las reglas de acceso. Con un token inválido
 * o vencido responde 401 de inmediato, aunque la ruta sea pública.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final RespuestaErrorJson respuestaError;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.validar(cabecera.substring(PREFIJO.length()));
            String rol = claims.get(JwtService.CLAIM_ROL, String.class);
            Number id = claims.get(JwtService.CLAIM_ID_USUARIO, Number.class);
            UsuarioAutenticado usuario = new UsuarioAutenticado(id == null ? null : id.longValue(), claims.getSubject(), rol);
            var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            respuestaError.escribir(request, response, HttpStatus.UNAUTHORIZED, "Token inválido o vencido");
            return;
        }
        chain.doFilter(request, response);
    }
}
