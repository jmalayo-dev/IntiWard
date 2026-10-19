package pe.intiguard.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato C6 con ms-inventario. El token del cliente se pasa como parámetro (no con un interceptor)
 * para que siga llegando aunque la llamada corra en otro hilo.
 */
@FeignClient(name = "ms-inventario")
public interface InventarioClient {

    /** 200 · 409 con detalles ["idProducto: 7"] si algo no alcanza (todo o nada). */
    @PostMapping("/stock/reservar")
    Reserva reservar(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody ReservarRequest request);

    /** Idempotente. */
    @PostMapping("/stock/confirmar")
    void confirmar(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Venta request);

    /** Idempotente: sin reservas activas responde 200 igual. */
    @PostMapping("/stock/liberar")
    void liberar(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody Venta request);

    record ReservarRequest(Long idVenta, List<Item> items) {
    }

    record Item(Long idProducto, int cantidad) {
    }

    record Venta(Long idVenta) {
    }

    record Reserva(Long idVenta, LocalDateTime expiraEn) {
    }
}
