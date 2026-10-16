package pe.intiguard.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.inventario.entity.MovimientoStock;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByIdProductoOrderByCreadoEnDesc(Long idProducto);
}
