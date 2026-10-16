package pe.intiguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService crearServicio(String secret) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        return new JwtService(props);
    }

    @Test
    void generaYValidaUnTokenConElIdYElRol() {
        JwtService jwt = crearServicio("clave-de-prueba-de-al-menos-32-caracteres!!");

        Claims claims = jwt.validar(jwt.generarToken(7L, "cliente@intiguard.pe", "CLIENTE"));

        assertThat(claims.getSubject()).isEqualTo("cliente@intiguard.pe");
        assertThat(claims.get(JwtService.CLAIM_ROL)).isEqualTo("CLIENTE");
        assertThat(claims.get(JwtService.CLAIM_ID_USUARIO, Number.class).longValue()).isEqualTo(7L);
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String token = crearServicio("clave-de-prueba-de-al-menos-32-caracteres!!").generarToken(1L, "a@b.pe", "ADMIN");

        assertThatThrownBy(() -> crearServicio("otra-clave-distinta-de-al-menos-32-caracteres").validar(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void noArrancaConUnaClaveVaciaOCorta() {
        assertThatThrownBy(() -> crearServicio("")).isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> crearServicio("corta")).isInstanceOf(IllegalStateException.class);
    }
}
