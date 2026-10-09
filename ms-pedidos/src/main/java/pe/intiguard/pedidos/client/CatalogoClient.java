package pe.intiguard.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign: "ms-catalogo" es el nombre en Eureka, no una IP.
 * PENDIENTE: InventarioClient (reservar/confirmar/liberar) y ComprobantesClient (emitir),
 * con circuit breaker de Resilience4j (timeout 2 s) configurado en application.yml.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    @GetMapping("/productos/{id}")
    ProductoDto obtenerProducto(@PathVariable("id") Long id);
}
