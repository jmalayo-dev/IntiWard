package pe.intiguard.inventario.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.intiguard.inventario.service.StockService;

/** CU09: cada minuto devuelve al disponible las reservas que pasaron sus 15 minutos sin confirmarse. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareaVencimientoReservas {

    private final StockService stockService;

    @Scheduled(fixedDelayString = "${intiguard.reserva.revisar-cada-ms:60000}", initialDelay = 30000)
    public void liberarVencidas() {
        try {
            int ventas = stockService.liberarVencidas();
            if (ventas > 0) {
                log.info("Reservas vencidas liberadas de {} ventas", ventas);
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudieron liberar las reservas vencidas: {}", ex.getMessage());
        }
    }
}
