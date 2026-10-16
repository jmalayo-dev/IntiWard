package pe.intiguard.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.intiguard.auth.dto.LoginRequest;
import pe.intiguard.auth.dto.LoginResponse;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.exception.CredencialesInvalidasException;
import pe.intiguard.auth.exception.CuentaBloqueadaException;
import pe.intiguard.auth.repository.UsuarioRepository;
import pe.intiguard.security.JwtProperties;
import pe.intiguard.security.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Las 6 reglas del login (plan, "Validaciones en el login"), una por una. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CLAVE = "Clave12345";

    @Mock
    private UsuarioRepository usuarioRepository;

    // BCrypt real con fuerza baja para que el test sea rápido
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private JwtService jwtService;
    private AuthService authService;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        JwtProperties props = new JwtProperties();
        props.setSecret("clave-de-prueba-de-al-menos-32-caracteres!!");
        jwtService = new JwtService(props);
        authService = new AuthService(usuarioRepository, encoder, jwtService, props);
        ana = Usuario.builder().id(7L).nombres("Ana").apellidos("Torres").correo("ana@intiguard.pe")
                .claveHash(encoder.encode(CLAVE)).rol(Rol.builder().id(2L).nombre("CLIENTE").build()).build();
    }

    private LoginResponse login(String clave) {
        return authService.login(new LoginRequest("ana@intiguard.pe", clave));
    }

    @Test
    void loginCorrectoDevuelveUnTokenConIdYRolYReiniciaElContador() {
        ana.setIntentosFallidos(3);
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        LoginResponse respuesta = login(CLAVE);

        Claims claims = jwtService.validar(respuesta.token());
        assertThat(claims.get(JwtService.CLAIM_ID_USUARIO, Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.get(JwtService.CLAIM_ROL)).isEqualTo("CLIENTE");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEnMinutos()).isEqualTo(60);
        assertThat(ana.getIntentosFallidos()).isZero();
    }

    @Test
    void correoInexistenteYClaveMalaDanElMismoMensaje() {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@intiguard.pe", CLAVE)))
                .isInstanceOf(CredencialesInvalidasException.class).hasMessage("Credenciales inválidas");

        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));
        assertThatThrownBy(() -> login("ClaveMala99"))
                .isInstanceOf(CredencialesInvalidasException.class).hasMessage("Credenciales inválidas");
    }

    @Test
    void claveMalaSumaUnIntento() {
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> login("ClaveMala99")).isInstanceOf(CredencialesInvalidasException.class);

        assertThat(ana.getIntentosFallidos()).isEqualTo(1);
        assertThat(ana.getBloqueadoHasta()).isNull();
    }

    @Test
    void elQuintoIntentoFallidoBloquea15Minutos() {
        ana.setIntentosFallidos(4);
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> login("ClaveMala99")).isInstanceOf(CuentaBloqueadaException.class);

        assertThat(ana.getIntentosFallidos()).isEqualTo(5);
        assertThat(ana.getBloqueadoHasta()).isBetween(LocalDateTime.now().plusMinutes(14), LocalDateTime.now().plusMinutes(16));
    }

    @Test
    void cuentaBloqueadaRechazaAunqueLaClaveSeaCorrecta() {
        ana.setIntentosFallidos(5);
        ana.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> login(CLAVE)).isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    void cuandoElBloqueoVenceSePuedeEntrarYElContadorVuelveACero() {
        ana.setIntentosFallidos(5);
        ana.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        assertThat(login(CLAVE).token()).isNotBlank();
        assertThat(ana.getIntentosFallidos()).isZero();
        assertThat(ana.getBloqueadoHasta()).isNull();
    }

    @Test
    void cuentaInactivaSeRechazaConElMensajeGenerico() {
        ana.setActivo(false);
        when(usuarioRepository.findByCorreo("ana@intiguard.pe")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> login(CLAVE))
                .isInstanceOf(CredencialesInvalidasException.class).hasMessage("Credenciales inválidas");
    }
}
