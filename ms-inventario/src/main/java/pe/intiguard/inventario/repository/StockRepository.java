package pe.intiguard.inventario.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.intiguard.inventario.entity.Stock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** La PK de Stock es el idProducto: se busca con findById(idProducto). */
public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findAllByOrderByIdProductoAsc();

    List<Stock> findByIdProductoIn(Collection<Long> ids);

    /**
     * SELECT ... FOR UPDATE, siempre en el mismo orden (por id) para que dos checkouts no se bloqueen
     * entre sí: así nadie reserva la misma última unidad.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.idProducto in :ids order by s.idProducto")
    List<Stock> bloquear(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.idProducto = :id")
    Optional<Stock> bloquearUno(@Param("id") Long id);
}
