package pe.intiguard.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.repository.RolRepository;
import pe.intiguard.auth.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El login de punta a punta: HTTP, validaciones, transacción real (H2) y seguridad.
 * Comprueba en especial que el contador de intentos se guarda aunque el login responda con error.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.admin.correo=",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class LoginIntegracionTest {

    private static final String CLAVE = "Clave12345";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
        Rol rolAdmin = rolRepository.save(Rol.builder().nombre("ADMIN").build());
        Rol rolCliente = rolRepository.save(Rol.builder().nombre("CLIENTE").build());
        admin = usuarioRepository.save(usuario("admin@intiguard.pe", rolAdmin));
        cliente = usuarioRepository.save(usuario("ana@intiguard.pe", rolCliente));
    }

    private Usuario usuario(String correo, Rol rol) {
        return Usuario.builder().nombres("N").apellidos("A").correo(correo)
                .claveHash(passwordEncoder.encode(CLAVE)).rol(rol).build();
    }

    private ResultActions login(String correo, String clave) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + clave + "\"}"));
    }

    private String token(String correo) throws Exception {
        String cuerpo = login(correo, CLAVE).andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(cuerpo);
        return nodo.get("token").asText();
    }

    @Test
    void loginCorrectoYElTokenSirveParaEntrar() throws Exception {
        login("admin@intiguard.pe", CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.expiraEnMinutos").value(60));

        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + token("admin@intiguard.pe")))
                .andExpect(status().isOk());
    }

    @Test
    void cincoIntentosFallidosBloqueanYElContadorQuedaGuardado() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login("ana@intiguard.pe", "ClaveMala99")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
        }
        login("ana@intiguard.pe", "ClaveMala99").andExpect(status().isLocked());
        login("ana@intiguard.pe", CLAVE).andExpect(status().isLocked());

        Usuario guardado = usuarioRepository.findByCorreo("ana@intiguard.pe").orElseThrow();
        assertThat(guardado.getIntentosFallidos()).isEqualTo(5);
        assertThat(guardado.getBloqueadoHasta()).isNotNull();
    }

    @Test
    void correoInexistenteRespondeIgualQueClaveMala() throws Exception {
        login("nadie@intiguard.pe", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void datosConFormatoInvalidoResponden400ConDetalle() throws Exception {
        login("esto-no-es-correo", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));
    }

    @Test
    void elListadoDeUsuariosEsSoloParaAdmin() throws Exception {
        String tokenCliente = token("ana@intiguard.pe");

        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/" + cliente.getId()).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk());
        mvc.perform(get("/usuarios/" + admin.getId()).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }
}
