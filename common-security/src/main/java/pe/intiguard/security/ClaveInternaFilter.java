package pe.intiguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Rutas internas entre servicios (reservar stock, emitir comprobante). Detrás del gateway ya responden 403
 * desde afuera; pero si el servicio tiene URL pública (Render, plan gratuito), cualquiera podría llamarlas directo.
 * Con intiguard.security.clave-interna definida, esas rutas exigen la cabecera X-Clave-Interna con la misma clave
 * (la manda ms-pedidos). Sin clave configurada no hace nada: es el modo local, con todo en la red de Docker.
 */
public class ClaveInternaFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Clave-Interna";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final byte[] clave;
    private final List<String> rutasInternas;
    private final RespuestaErrorJson respuestaError;

    public ClaveInternaFilter(SeguridadProperties seguridad, RespuestaErrorJson respuestaError) {
        String configurada = seguridad.getClaveInterna();
        this.clave = StringUtils.hasText(configurada) ? configurada.getBytes(StandardCharsets.UTF_8) : null;
        this.rutasInternas = seguridad.getRutasInternas();
        this.respuestaError = respuestaError;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (clave != null && esInterna(request) && !claveCorrecta(request.getHeader(CABECERA))) {
            respuestaError.escribir(request, response, HttpStatus.FORBIDDEN, "Ruta de uso interno entre servicios");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean claveCorrecta(String recibida) {
        // Comparación en tiempo constante: no revela cuántos caracteres coinciden
        return recibida != null && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8));
    }

    private boolean esInterna(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        for (String regla : rutasInternas) {
            String[] partes = regla.trim().split("\\s+", 2);
            boolean coincide = partes.length == 2
                    ? request.getMethod().equalsIgnoreCase(partes[0]) && matcher.match(partes[1], ruta)
                    : matcher.match(partes[0], ruta);
            if (coincide) return true;
        }
        return false;
    }
}
