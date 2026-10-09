package pe.intiguard.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.catalogo.entity.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaIdAndActivoTrue(Integer idCategoria);
}
