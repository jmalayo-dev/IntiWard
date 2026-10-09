package pe.intiguard.inventario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.inventario.dto.StockResponse;
import pe.intiguard.inventario.service.StockService;

/** PENDIENTE: POST /stock/reservar, /stock/confirmar, /stock/liberar y GET /movimientos. */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/{idProducto}")
    public ResponseEntity<StockResponse> obtener(@PathVariable Long idProducto) {
        return ResponseEntity.ok(stockService.obtener(idProducto));
    }
}
