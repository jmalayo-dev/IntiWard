package pe.intiguard.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** "ms-catalogo" es el nombre en Eureka, no una IP. GET /productos/{id} es público: 404 si no existe. */
@FeignClient(name = "ms-catalogo", url = "${intiguard.servicios.catalogo:}")
public interface CatalogoClient {

    @GetMapping("/productos/{id}")
    ProductoDto obtenerProducto(@PathVariable("id") Long id);
}
