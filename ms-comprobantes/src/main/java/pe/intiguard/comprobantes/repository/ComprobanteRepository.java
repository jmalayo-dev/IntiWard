package pe.intiguard.comprobantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.comprobantes.entity.Comprobante;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByIdVenta(Long idVenta);

    /** Del más nuevo al más antiguo. */
    List<Comprobante> findAllByOrderByEmitidoEnDescIdDesc();

    /** Los de un cliente (usa el índice ix_comprobante_usuario). */
    List<Comprobante> findByIdUsuarioOrderByEmitidoEnDescIdDesc(Long idUsuario);

    /** Rango [desde, hasta): usa el índice ix_comprobante_emision. */
    List<Comprobante> findByEmitidoEnGreaterThanEqualAndEmitidoEnLessThanOrderByEmitidoEnAscIdAsc(LocalDateTime desde,
                                                                                                  LocalDateTime hasta);
}
