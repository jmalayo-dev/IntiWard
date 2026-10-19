package pe.intiguard.comprobantes.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.intiguard.comprobantes.entity.SerieCorrelativo;
import pe.intiguard.comprobantes.entity.TipoComprobante;

import java.util.Optional;

public interface SerieCorrelativoRepository extends JpaRepository<SerieCorrelativo, String> {

    /**
     * Serie vigente de un tipo, bloqueada con SELECT ... FOR UPDATE hasta el fin de la transacción.
     * Si hay varias series del mismo tipo, toma la primera en orden alfabético.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SerieCorrelativo> findFirstByTipoOrderBySerieAsc(TipoComprobante tipo);
}
