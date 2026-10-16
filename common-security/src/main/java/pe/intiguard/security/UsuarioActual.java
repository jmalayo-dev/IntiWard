package pe.intiguard.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Contrato C2: el usuario de la petición en curso, leído del token por JwtAuthFilter.
 * Se inyecta en cualquier servicio: {@code usuarioActual.id()}. Nadie parsea el token por su cuenta.
 */
public class UsuarioActual {

    /** Vacío si la petición no trae token (rutas públicas o EXIGIR_TOKEN=false). */
    public Optional<UsuarioAutenticado> obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    /** Lanza AuthenticationCredentialsNotFoundException (401) si no hay usuario autenticado. */
    public UsuarioAutenticado requerido() {
        return obtener().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Se requiere iniciar sesión"));
    }

    public Long id() {
        return requerido().id();
    }

    public String correo() {
        return requerido().correo();
    }

    public String rol() {
        return requerido().rol();
    }

    public boolean esAdmin() {
        return obtener().map(UsuarioAutenticado::esAdmin).orElse(false);
    }
}
