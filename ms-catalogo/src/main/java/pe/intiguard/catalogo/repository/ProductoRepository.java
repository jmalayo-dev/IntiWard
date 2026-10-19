package pe.intiguard.catalogo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import pe.intiguard.catalogo.entity.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaIdAndActivoTrue(Long idCategoria);

    /** Marca y categoría en la misma consulta; las especificaciones llegan por lotes (@BatchSize). */
    @Override
    @EntityGraph(attributePaths = {"marca", "categoria"})
    Page<Producto> findAll(Specification<Producto> spec, Pageable pageable);

    /** [idMarca, cantidad de productos activos] */
    @Query("select p.marca.id, count(p) from Producto p where p.activo = true group by p.marca.id")
    List<Object[]> contarActivosPorMarca();

    /** [idCategoria, cantidad de productos activos] */
    @Query("select p.categoria.id, count(p) from Producto p where p.activo = true group by p.categoria.id")
    List<Object[]> contarActivosPorCategoria();
}
