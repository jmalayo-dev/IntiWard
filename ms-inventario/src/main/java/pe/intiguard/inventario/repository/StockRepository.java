package pe.intiguard.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.inventario.entity.Stock;

/** La PK de Stock es el idProducto: se busca con findById(idProducto). */
public interface StockRepository extends JpaRepository<Stock, Long> {
}
