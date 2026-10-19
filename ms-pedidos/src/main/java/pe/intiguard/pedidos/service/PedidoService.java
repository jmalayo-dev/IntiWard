package pe.intiguard.pedidos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.entity.EstadoVenta;
import pe.intiguard.pedidos.entity.Venta;
import pe.intiguard.pedidos.exception.RecursoNoEncontradoException;
import pe.intiguard.pedidos.mapper.VentaMapper;
import pe.intiguard.pedidos.repository.VentaRepository;
import pe.intiguard.security.UsuarioActual;

import java.time.LocalDateTime;
import java.util.List;

/** Consulta de ventas con control de dueño: un CLIENTE ve las suyas; el ADMIN, todas. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {

    private final VentaRepository ventaRepository;
    private final UsuarioActual usuarioActual;

    /** De la más nueva a la más antigua, con sus detalles. */
    public List<VentaResponse> listar() {
        List<Venta> ventas = usuarioActual.esAdmin()
                ? ventaRepository.findAllByOrderByFechaVentaDescIdDesc()
                : ventaRepository.findByIdUsuarioOrderByFechaVentaDescIdDesc(usuarioActual.id());
        return ventas.stream().map(VentaMapper::toResponse).toList();
    }

    /** 403 si es de otro cliente. 404 si no existe. */
    public VentaResponse obtener(Long id) {
        Venta venta = ventaRepository.findWithDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + id + " no existe"));
        if (!usuarioActual.esAdmin() && !venta.getIdUsuario().equals(usuarioActual.id())) {
            throw new AccessDeniedException("El pedido es de otro cliente");
        }
        return VentaMapper.toResponse(venta);
    }

    /** CU09: una venta PENDIENTE por más de los minutos indicados ya perdió su reserva: se anula. */
    @Transactional
    public int anularPendientesVencidas(int minutos) {
        List<Venta> vencidas = ventaRepository.findByEstadoAndFechaVentaBefore(EstadoVenta.PENDIENTE,
                LocalDateTime.now().minusMinutes(minutos));
        vencidas.forEach(v -> v.setEstado(EstadoVenta.ANULADA));
        return vencidas.size();
    }
}
