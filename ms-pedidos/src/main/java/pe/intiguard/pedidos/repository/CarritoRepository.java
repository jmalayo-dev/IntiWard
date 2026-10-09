package pe.intiguard.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.intiguard.pedidos.entity.Carrito;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByIdUsuario(Long idUsuario);
}
