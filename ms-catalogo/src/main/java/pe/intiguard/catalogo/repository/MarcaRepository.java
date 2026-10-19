package pe.intiguard.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.catalogo.entity.Marca;

import java.util.List;
import java.util.Optional;

public interface MarcaRepository extends JpaRepository<Marca, Long> {

    List<Marca> findAllByOrderByNombreAsc();

    List<Marca> findByActivoTrueOrderByNombreAsc();

    Optional<Marca> findFirstByNombreIgnoreCase(String nombre);
}
