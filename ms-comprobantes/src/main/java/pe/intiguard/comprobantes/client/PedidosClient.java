package pe.intiguard.comprobantes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * ms-pedidos por Eureka. GET /pedidos/{id} exige ser el dueño o ADMIN:
 * ReenvioTokenConfig reenvía el token de quien pidió el PDF.
 */
@FeignClient(name = "ms-pedidos")
public interface PedidosClient {

    @GetMapping("/pedidos/{id}")
    VentaDto obtenerVenta(@PathVariable("id") Long id);
}
