package pe.intiguard.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.catalogo.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}
