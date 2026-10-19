package pe.intiguard.pedidos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.pedidos.entity.*;
import pe.intiguard.pedidos.repository.VentaRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pasos locales del checkout, cada uno en su propia transacción: la saga no puede ser una sola
 * transacción porque entre paso y paso se llama a otros servicios.
 */
@Service
@RequiredArgsConstructor
public class VentaRegistro {

    private final VentaRepository ventaRepository;

    public record Linea(Long idProducto, String nombre, BigDecimal precio, int cantidad) {
    }

    /** Paso 3: venta PENDIENTE con sus detalles. Así existe el idVenta para reservar y emitir. */
    @Transactional
    public Venta crearPendiente(Long idUsuario, TipoComprobante tipo, List<Linea> lineas) {
        Venta venta = Venta.builder().idUsuario(idUsuario).tipoComprobante(tipo).build();
        BigDecimal total = BigDecimal.ZERO;
        for (Linea l : lineas) {
            venta.getDetalles().add(DetalleVenta.builder().venta(venta).idProducto(l.idProducto())
                    .nombreProducto(l.nombre()).precioUnitario(l.precio()).cantidad(l.cantidad()).build());
            total = total.add(l.precio().multiply(BigDecimal.valueOf(l.cantidad())));
        }
        venta.asignarMontos(total);
        return ventaRepository.saveAndFlush(venta);
    }

    @Transactional
    public Venta cambiarEstado(Long idVenta, EstadoVenta estado) {
        Venta venta = ventaRepository.findWithDetallesById(idVenta).orElseThrow();
        venta.setEstado(estado);
        return ventaRepository.saveAndFlush(venta);
    }
}
