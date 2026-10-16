package pe.intiguard.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba la autoconfiguración completa en una mini aplicación con tres endpoints. */
class JwtAuthFilterTest {

    private static final String CLAVE = "clave-de-prueba-de-al-menos-32-caracteres!!";

    @SpringBootApplication
    @RestController
    static class AppDePrueba {

        private final UsuarioActual usuarioActual;

        AppDePrueba(UsuarioActual usuarioActual) {
            this.usuarioActual = usuarioActual;
        }

        @GetMapping("/publico")
        String publico() {
            return "ok";
        }

        @GetMapping("/mio")
        String mio() {
            return "usuario " + usuarioActual.id();
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "admin";
        }
    }

    abstract static class Base {

        @Autowired
        MockMvc mvc;

        @Autowired
        JwtService jwtService;

        String bearer(Long id, String rol) {
            return "Bearer " + jwtService.generarToken(id, "u" + id + "@intiguard.pe", rol);
        }
    }

    @Nested
    @SpringBootTest(classes = AppDePrueba.class, properties = {
            "intiguard.jwt.secret=" + CLAVE,
            "intiguard.security.exigir-token=true",
            "intiguard.security.rutas-publicas=GET /publico"})
    @AutoConfigureMockMvc
    class ConTokenObligatorio extends Base {

        @Test
        void rutaPublicaSinToken() throws Exception {
            mvc.perform(get("/publico")).andExpect(status().isOk());
        }

        @Test
        void rutaProtegidaSinTokenResponde401EnJson() throws Exception {
            mvc.perform(get("/mio"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.ruta").value("/mio"));
        }

        @Test
        void tokenValidoDejaPasarYExponeElIdDelUsuario() throws Exception {
            mvc.perform(get("/mio").header("Authorization", bearer(7L, "CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("usuario 7"));
        }

        @Test
        void tokenInvalidoResponde401AunqueLaRutaSeaPublica() throws Exception {
            mvc.perform(get("/publico").header("Authorization", "Bearer token-falso"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Token inválido o vencido"));
        }

        @Test
        void clienteEnRutaDeAdminResponde403() throws Exception {
            mvc.perform(get("/admin").header("Authorization", bearer(7L, "CLIENTE")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void adminEnRutaDeAdmin() throws Exception {
            mvc.perform(get("/admin").header("Authorization", bearer(1L, "ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(classes = AppDePrueba.class, properties = "intiguard.jwt.secret=" + CLAVE)
    @AutoConfigureMockMvc
    class ConTokenOpcional extends Base {

        @Test
        void sinTokenNoBloqueaSalvoDondeSePideUnRol() throws Exception {
            mvc.perform(get("/publico")).andExpect(status().isOk());
            // @PreAuthorize sigue exigiendo el rol: un anónimo recibe 401 ("inicia sesión"), no 403
            mvc.perform(get("/admin")).andExpect(status().isUnauthorized());
        }

        @Test
        void siLlegaTokenSeUsa() throws Exception {
            mvc.perform(get("/mio").header("Authorization", bearer(9L, "CLIENTE")))
                    .andExpect(content().string("usuario 9"));
        }
    }
}
