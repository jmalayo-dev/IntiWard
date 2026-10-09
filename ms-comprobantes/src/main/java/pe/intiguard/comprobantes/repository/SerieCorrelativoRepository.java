package pe.intiguard.comprobantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.comprobantes.entity.SerieCorrelativo;
import pe.intiguard.comprobantes.entity.TipoComprobante;

import java.util.Optional;

public interface SerieCorrelativoRepository extends JpaRepository<SerieCorrelativo, Integer> {

    Optional<SerieCorrelativo> findByTipo(TipoComprobante tipo);
}
