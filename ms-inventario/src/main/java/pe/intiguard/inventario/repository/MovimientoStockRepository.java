package pe.intiguard.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.inventario.entity.MovimientoStock;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /** Del más reciente al más antiguo. */
    List<MovimientoStock> findByIdProductoOrderByCreadoEnDescIdDesc(Long idProducto);

    List<MovimientoStock> findAllByOrderByCreadoEnDescIdDesc();
}
