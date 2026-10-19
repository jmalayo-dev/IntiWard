package pe.intiguard.pedidos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.intiguard.pedidos.service.PedidoService;

/** CU09: anula las ventas que quedaron PENDIENTE (por ejemplo, si el servicio se cayó a mitad del checkout). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareaVentasPendientes {

    private final PedidoService pedidoService;

    @Value("${intiguard.checkout.minutos-pendiente:15}")
    private int minutos;

    @Scheduled(fixedDelayString = "${intiguard.checkout.revisar-cada-ms:60000}", initialDelay = 30000)
    public void anular() {
        try {
            int anuladas = pedidoService.anularPendientesVencidas(minutos);
            if (anuladas > 0) {
                log.info("{} ventas PENDIENTE de más de {} min pasaron a ANULADA", anuladas, minutos);
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudieron revisar las ventas pendientes: {}", ex.getMessage());
        }
    }
}
