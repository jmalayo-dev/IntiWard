package pe.intiguard.comprobantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.comprobantes.entity.Comprobante;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByIdVenta(Long idVenta);
}
