package pe.intiguard.pedidos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.exception.RecursoNoEncontradoException;
import pe.intiguard.pedidos.mapper.VentaMapper;
import pe.intiguard.pedidos.repository.VentaRepository;

import java.util.List;

/**
 * PENDIENTE (semana 4-5): checkout(idUsuario) con la saga por compensación:
 *  1. Leer el carrito.  2. catalogoClient: precio actual.  3. inventarioClient.reservar.
 *  4. Guardar Venta PENDIENTE.  5. comprobantesClient.emitir.  6. inventarioClient.confirmar y Venta CONFIRMADA.
 *  Si 5 falla o tarda más de 2 s: inventarioClient.liberar, Venta ANULADA y error claro al cliente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {

    private final VentaRepository ventaRepository;

    public List<VentaResponse> listar() {
        return ventaRepository.findAll().stream().map(VentaMapper::toResponse).toList();
    }

    public VentaResponse obtener(Long id) {
        return ventaRepository.findById(id)
                .map(VentaMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + id + " no existe"));
    }
}
