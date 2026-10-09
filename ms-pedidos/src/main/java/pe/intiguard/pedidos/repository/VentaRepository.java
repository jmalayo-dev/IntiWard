package pe.intiguard.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.pedidos.entity.Venta;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByIdUsuarioOrderByFechaDesc(Long idUsuario);
}
