package pe.intiguard.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.auth.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}
