package pe.intiguard.inventario.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** ms-catalogo por Eureka (contrato C5): 404 si el producto no existe. GET /productos/{id} es público. */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    @GetMapping("/productos/{id}")
    Producto obtenerProducto(@PathVariable("id") Long id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Producto(Long id, String nombre, boolean activo) {
    }
}
