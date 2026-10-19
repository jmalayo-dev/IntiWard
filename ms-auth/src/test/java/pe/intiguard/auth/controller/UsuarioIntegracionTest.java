package pe.intiguard.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.repository.RolRepository;
import pe.intiguard.auth.repository.UsuarioRepository;
import pe.intiguard.security.JwtService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registro, perfil propio y administración de usuarios contra H2. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuarios;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.admin.correo=",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class UsuarioIntegracionTest {

    private static final String CLAVE = "Clave12345";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Usuario admin;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
        Rol rolAdmin = rolRepository.save(Rol.builder().nombre("ADMIN").build());
        Rol rolCliente = rolRepository.save(Rol.builder().nombre("CLIENTE").build());
        admin = usuarioRepository.save(Usuario.builder().nombres("Admin").apellidos("IntiGuard").correo("admin@intiguard.pe")
                .claveHash(passwordEncoder.encode(CLAVE)).rol(rolAdmin).build());
        cliente = usuarioRepository.save(Usuario.builder().nombres("Ana").apellidos("Torres").correo("ana@intiguard.pe")
                .claveHash(passwordEncoder.encode(CLAVE)).rol(rolCliente).build());
    }

    private String token(Usuario u) {
        return "Bearer " + jwtService.generarToken(u.getId(), u.getCorreo(), u.getRol().getNombre());
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, Usuario quien, String cuerpo) throws Exception {
        if (quien != null) peticion.header("Authorization", token(quien));
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(peticion);
    }

    @Test
    void registroCreaSiempreUnClienteYElCorreoNoSeRepite() throws Exception {
        String cuerpo = """
                {"nombres":"Luis","apellidos":"Ramos","correo":"luis@gmail.com","password":"Clave12345","rol":"ADMIN"}""";
        enviar(post("/auth/registro"), null, cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.claveHash").doesNotExist());

        enviar(post("/auth/registro"), null, cuerpo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));

        // El cliente nuevo puede iniciar sesión y queda guardado su último acceso
        enviar(post("/auth/login"), null, "{\"correo\":\"luis@gmail.com\",\"password\":\"Clave12345\"}")
                .andExpect(status().isOk());
        assertThat(usuarioRepository.findByCorreo("luis@gmail.com").orElseThrow().getUltimoAcceso()).isNotNull();
    }

    @Test
    void registroConDatosInvalidosDevuelveUnDetallePorCampo() throws Exception {
        enviar(post("/auth/registro"), null, "{\"nombres\":\"\",\"apellidos\":\"R\",\"correo\":\"malo\",\"password\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(3));
    }

    @Test
    void elUsuarioVeYEditaSuPerfilSinCambiarCorreoNiRol() throws Exception {
        enviar(get("/usuarios/me"), null, null).andExpect(status().isUnauthorized());
        enviar(get("/usuarios/me"), cliente, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@intiguard.pe"));

        enviar(put("/usuarios/me"), cliente, """
                {"telefono":"987654321","fotoUrl":"https://x.pe/a.png","correo":"otro@x.pe","rol":"ADMIN"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("987654321"))
                .andExpect(jsonPath("$.fotoUrl").value("https://x.pe/a.png"))
                .andExpect(jsonPath("$.correo").value("ana@intiguard.pe"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // fotoUrl: null quita la foto; un campo que no viene no se toca
        enviar(put("/usuarios/me"), cliente, "{\"fotoUrl\":null}")
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.telefono").value("987654321"));

        enviar(put("/usuarios/me"), cliente, "{\"password\":\"corta\",\"nombres\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));

        enviar(put("/usuarios/me"), cliente, "{\"password\":\"NuevaClave99\"}").andExpect(status().isOk());
        enviar(post("/auth/login"), null, "{\"correo\":\"ana@intiguard.pe\",\"password\":\"NuevaClave99\"}")
                .andExpect(status().isOk());
    }

    @Test
    void elAdminCreaEditaYDesactivaCuentas() throws Exception {
        enviar(post("/usuarios"), cliente, """
                {"nombres":"X","apellidos":"Y","correo":"x@intiguard.pe","password":"Clave12345"}""")
                .andExpect(status().isForbidden());

        String creado = enviar(post("/usuarios"), admin, """
                {"nombres":"Eva","apellidos":"Soto","correo":"eva@intiguard.pe","password":"Clave12345","rol":"ADMIN"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(creado.replaceAll(".*\"id\":(\\d+).*", "$1"));

        enviar(put("/usuarios/" + id), admin, "{\"correo\":\"ana@intiguard.pe\"}").andExpect(status().isConflict());
        enviar(put("/usuarios/" + id), admin, "{\"rol\":\"CLIENTE\",\"apellidos\":\"Soto Díaz\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.apellidos").value("Soto Díaz"));

        enviar(delete("/usuarios/" + id), admin, null).andExpect(status().isNoContent());
        assertThat(usuarioRepository.findById(id).orElseThrow().isActivo()).isFalse();
        enviar(delete("/usuarios/999999"), admin, null).andExpect(status().isNotFound());

        // GET /usuarios incluye a los inactivos
        enviar(get("/usuarios"), admin, null).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void reactivarUnaCuentaTambienLevantaElBloqueo() throws Exception {
        cliente.setActivo(false);
        cliente.setIntentosFallidos(5);
        cliente.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        usuarioRepository.save(cliente);

        enviar(get("/usuarios/" + cliente.getId()), admin, null)
                .andExpect(jsonPath("$.bloqueadoHasta").isNotEmpty());

        enviar(put("/usuarios/" + cliente.getId()), admin, "{\"activo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.bloqueadoHasta").value(nullValue()));

        Usuario guardado = usuarioRepository.findById(cliente.getId()).orElseThrow();
        assertThat(guardado.getIntentosFallidos()).isZero();
        assertThat(guardado.getBloqueadoHasta()).isNull();
    }

    @Test
    void unBloqueoVencidoNoSeMuestra() throws Exception {
        cliente.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.save(cliente);
        enviar(get("/usuarios/me"), cliente, null).andExpect(jsonPath("$.bloqueadoHasta").value(nullValue()));
    }
}
