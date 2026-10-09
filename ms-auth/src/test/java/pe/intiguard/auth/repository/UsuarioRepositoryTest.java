package pe.intiguard.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba de la capa de datos con H2 en memoria. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void encuentraUnUsuarioPorCorreo() {
        Rol cliente = rolRepository.save(Rol.builder().nombre("CLIENTE").build());
        usuarioRepository.save(Usuario.builder()
                .nombre("Ana Torres").correo("ana@intiguard.pe").password("hash").rol(cliente).build());

        assertThat(usuarioRepository.findByCorreo("ana@intiguard.pe"))
                .isPresent()
                .get().extracting(u -> u.getRol().getNombre()).isEqualTo("CLIENTE");
        assertThat(usuarioRepository.existsByCorreo("nadie@intiguard.pe")).isFalse();
    }
}
