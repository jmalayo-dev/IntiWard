package pe.intiguard.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.auth.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
