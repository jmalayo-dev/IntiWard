package pe.intiguard.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.inventario.entity.EstadoReserva;
import pe.intiguard.inventario.entity.Reserva;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByIdVenta(Long idVenta);

    List<Reserva> findByIdVentaAndEstado(Long idVenta, EstadoReserva estado);

    /** Para la tarea de vencimiento: usa el índice ix_reserva_vencimiento (estado, expira_en). */
    List<Reserva> findByEstadoAndExpiraEnBefore(EstadoReserva estado, LocalDateTime limite);
}
