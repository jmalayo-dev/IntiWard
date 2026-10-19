package pe.intiguard.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.auth.dto.LoginRequest;
import pe.intiguard.auth.dto.LoginResponse;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.exception.CredencialesInvalidasException;
import pe.intiguard.auth.exception.CuentaBloqueadaException;
import pe.intiguard.auth.repository.UsuarioRepository;
import pe.intiguard.security.JwtProperties;
import pe.intiguard.security.JwtService;

import java.time.LocalDateTime;

/**
 * Login (contrato C3) con las reglas del plan:
 * mensaje genérico (401), 5 intentos fallidos seguidos bloquean 15 minutos (423),
 * cuenta inactiva rechazada, JWT de 1 hora con idUsuario y rol, y la clave nunca en el log.
 */
@Slf4j
@Service
public class AuthService {

    static final int MAX_INTENTOS = 5;
    static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    /** Para comparar contra algo cuando el correo no existe y responder en el mismo tiempo que con un correo real. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JwtProperties jwtProperties) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.hashFicticio = passwordEncoder.encode("clave-ficticia-para-igualar-tiempos");
    }

    // Sin noRollbackFor, la excepción desharía el intento fallido recién sumado y el bloqueo nunca llegaría
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaException.class})
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo()).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(request.password(), hashFicticio);
            throw new CredencialesInvalidasException();
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getBloqueadoHasta() != null) {
            if (usuario.getBloqueadoHasta().isAfter(ahora)) {
                throw new CuentaBloqueadaException();
            }
            // El bloqueo ya venció: se empieza de cero
            usuario.levantarBloqueo();
        }

        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (!passwordEncoder.matches(request.password(), usuario.getClaveHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS) {
                usuario.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                log.warn("Cuenta bloqueada {} min por {} intentos fallidos: {}", MINUTOS_BLOQUEO, intentos, usuario.getCorreo());
                throw new CuentaBloqueadaException();
            }
            throw new CredencialesInvalidasException();
        }

        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(ahora);
        String rol = usuario.getRol().getNombre();
        String token = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), rol);
        log.info("Login correcto: {} ({})", usuario.getCorreo(), rol);
        return new LoginResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), rol);
    }
}
