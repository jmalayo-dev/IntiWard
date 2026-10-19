package pe.intiguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaveInternaFilterTest {

    private final RespuestaErrorJson respuesta = new RespuestaErrorJson(new ObjectMapper().registerModule(new JavaTimeModule()));

    private ClaveInternaFilter filtro(String clave) {
        SeguridadProperties props = new SeguridadProperties();
        props.setRutasInternas(List.of("POST /stock/reservar", "POST /comprobantes"));
        props.setClaveInterna(clave);
        return new ClaveInternaFilter(props, respuesta);
    }

    /** Status de la respuesta, o 0 si dejó pasar la petición. */
    private int ejecutar(ClaveInternaFilter filtro, String metodo, String ruta, String cabecera) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        if (cabecera != null) request.addHeader(ClaveInternaFilter.CABECERA, cabecera);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        return chain.getRequest() != null ? 0 : response.getStatus();
    }

    @Test
    void conClaveLaRutaInternaExigeLaCabecera() throws Exception {
        ClaveInternaFilter f = filtro("secreto-compartido");
        assertThat(ejecutar(f, "POST", "/stock/reservar", null)).isEqualTo(403);
        assertThat(ejecutar(f, "POST", "/comprobantes", "otra")).isEqualTo(403);
        assertThat(ejecutar(f, "POST", "/stock/reservar", "secreto-compartido")).isZero();
    }

    @Test
    void lasRutasNoInternasNoCambian() throws Exception {
        ClaveInternaFilter f = filtro("secreto-compartido");
        assertThat(ejecutar(f, "GET", "/comprobantes", null)).isZero();
        assertThat(ejecutar(f, "POST", "/stock/entrada", null)).isZero();
    }

    @Test
    void sinClaveConfiguradaNoControlaNada() throws Exception {
        assertThat(ejecutar(filtro(""), "POST", "/stock/reservar", null)).isZero();
        assertThat(ejecutar(filtro(null), "POST", "/comprobantes", null)).isZero();
    }
}
