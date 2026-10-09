package pe.intiguard.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.inventario.entity.Stock;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByIdProducto(Long idProducto);
}
