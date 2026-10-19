package pe.intiguard.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.catalogo.entity.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombreAsc();

    List<Categoria> findByActivoTrueOrderByNombreAsc();

    Optional<Categoria> findFirstByNombreIgnoreCase(String nombre);
}
