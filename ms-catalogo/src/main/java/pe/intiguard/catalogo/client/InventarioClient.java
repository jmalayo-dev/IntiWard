package pe.intiguard.catalogo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/** ms-inventario por Eureka. GET /stock es público: devuelve todas las filas de stock. */
@FeignClient(name = "ms-inventario", url = "${intiguard.servicios.inventario:}")
public interface InventarioClient {

    @GetMapping("/stock")
    List<StockDto> listarStock();
}
