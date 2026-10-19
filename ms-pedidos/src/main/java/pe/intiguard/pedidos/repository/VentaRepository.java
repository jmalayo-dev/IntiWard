package pe.intiguard.pedidos.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.pedidos.entity.EstadoVenta;
import pe.intiguard.pedidos.entity.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Con los detalles en la misma consulta (sin una consulta por venta). */
    @EntityGraph(attributePaths = "detalles")
    List<Venta> findByIdUsuarioOrderByFechaVentaDescIdDesc(Long idUsuario);

    @EntityGraph(attributePaths = "detalles")
    List<Venta> findAllByOrderByFechaVentaDescIdDesc();

    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findWithDetallesById(Long id);

    List<Venta> findByIdUsuarioOrderByFechaVentaDesc(Long idUsuario);

    /** Ventas que siguen PENDIENTE después del límite: su reserva ya venció (CU09). */
    List<Venta> findByEstadoAndFechaVentaBefore(EstadoVenta estado, LocalDateTime limite);
}
