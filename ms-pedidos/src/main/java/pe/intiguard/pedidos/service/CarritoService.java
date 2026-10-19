package pe.intiguard.pedidos.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.pedidos.client.CatalogoClient;
import pe.intiguard.pedidos.client.ProductoDto;
import pe.intiguard.pedidos.dto.CarritoResponse;
import pe.intiguard.pedidos.entity.Carrito;
import pe.intiguard.pedidos.entity.ItemCarrito;
import pe.intiguard.pedidos.exception.NegocioException;
import pe.intiguard.pedidos.exception.RecursoNoEncontradoException;
import pe.intiguard.pedidos.repository.CarritoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un carrito por cliente (el del token), una línea por producto. Solo guarda producto y cantidad:
 * nombre, marca, precio e imagen se piden a ms-catalogo al armar la respuesta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CatalogoClient catalogoClient;

    public CarritoResponse ver(Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario).map(this::aResponse).orElse(vacio());
    }

    /** Si el producto ya estaba, suma la cantidad. 404 si no existe o está inactivo. */
    @Transactional
    public CarritoResponse agregar(Long idUsuario, Long idProducto, int cantidad) {
        ProductoDto producto = productoVendible(idProducto);
        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> carritoRepository.save(Carrito.builder().idUsuario(idUsuario).build()));
        linea(carrito, idProducto).ifPresentOrElse(
                l -> l.setCantidad(l.getCantidad() + cantidad),
                () -> carrito.getItems().add(ItemCarrito.builder().carrito(carrito).idProducto(producto.id())
                        .cantidad(cantidad).build()));
        carritoRepository.saveAndFlush(carrito);
        return aResponse(carrito);
    }

    /** Cantidad absoluta. 404 si el producto no está en el carrito. */
    @Transactional
    public CarritoResponse cambiarCantidad(Long idUsuario, Long idProducto, int cantidad) {
        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> fueraDelCarrito(idProducto));
        linea(carrito, idProducto).orElseThrow(() -> fueraDelCarrito(idProducto)).setCantidad(cantidad);
        carritoRepository.saveAndFlush(carrito);
        return aResponse(carrito);
    }

    /** Si el producto no estaba, no hace nada y responde igual. */
    @Transactional
    public CarritoResponse quitar(Long idUsuario, Long idProducto) {
        Optional<Carrito> carrito = carritoRepository.findByIdUsuario(idUsuario);
        if (carrito.isEmpty()) {
            return vacio();
        }
        carrito.get().getItems().removeIf(l -> l.getIdProducto().equals(idProducto));
        carritoRepository.saveAndFlush(carrito.get());
        return aResponse(carrito.get());
    }

    /** Producto y cantidad de cada línea, para el checkout. Vacío si no hay carrito. */
    public List<Linea> lineas(Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario)
                .map(c -> c.getItems().stream().map(l -> new Linea(l.getIdProducto(), l.getCantidad())).toList())
                .orElse(List.of());
    }

    public record Linea(Long idProducto, int cantidad) {
    }

    /** Lo usa el checkout al confirmar la venta. */
    @Transactional
    public void vaciar(Long idUsuario) {
        carritoRepository.findByIdUsuario(idUsuario).ifPresent(c -> c.getItems().clear());
    }

    private CarritoResponse aResponse(Carrito carrito) {
        List<CarritoResponse.Item> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrito linea : carrito.getItems()) {
            ProductoDto p = producto(linea.getIdProducto());
            if (p == null) {
                continue; // El producto ya no existe en catálogo: la línea no se muestra
            }
            BigDecimal importe = p.precio().multiply(BigDecimal.valueOf(linea.getCantidad()));
            items.add(new CarritoResponse.Item(p.id(), p.nombre(), p.marca(), p.precio(), p.imagenUrl(), linea.getCantidad(), importe));
            total = total.add(importe);
        }
        return new CarritoResponse(items, total);
    }

    private static CarritoResponse vacio() {
        return new CarritoResponse(List.of(), BigDecimal.ZERO);
    }

    private static Optional<ItemCarrito> linea(Carrito carrito, Long idProducto) {
        return carrito.getItems().stream().filter(l -> l.getIdProducto().equals(idProducto)).findFirst();
    }

    private ProductoDto productoVendible(Long idProducto) {
        ProductoDto p = producto(idProducto);
        if (p == null || !p.estaActivo()) {
            throw new RecursoNoEncontradoException("Producto " + idProducto + " no encontrado");
        }
        return p;
    }

    /** null si el producto no existe; 503 si ms-catalogo no responde. */
    private ProductoDto producto(Long idProducto) {
        try {
            return catalogoClient.obtenerProducto(idProducto);
        } catch (FeignException.NotFound ex) {
            return null;
        } catch (RuntimeException ex) {
            log.warn("ms-catalogo no respondió por el producto {}: {}", idProducto, ex.getMessage());
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo no está disponible en este momento. Intenta de nuevo en unos segundos");
        }
    }

    private static RecursoNoEncontradoException fueraDelCarrito(Long idProducto) {
        return new RecursoNoEncontradoException("El producto " + idProducto + " no está en el carrito");
    }
}
