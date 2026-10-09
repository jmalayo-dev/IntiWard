package pe.intiguard.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.catalogo.entity.Marca;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {
}
