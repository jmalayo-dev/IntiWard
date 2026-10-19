package pe.intiguard.inventario.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.inventario.dto.*;
import pe.intiguard.inventario.service.StockService;

import java.util.List;

/**
 * GET públicos. Entrada y ajuste: solo ADMIN. reservar, confirmar y liberar son internos:
 * el gateway los bloquea (403) y solo los llama ms-pedidos dentro de la red.
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** GET /stock?ids=1,2,3: una entrada por id (en 0 si no tiene stock). Sin ids: todas las filas. */
    @GetMapping
    public ResponseEntity<List<StockResponse>> listar(@RequestParam(required = false) List<Long> ids) {
        return ResponseEntity.ok(stockService.listar(ids));
    }

    @GetMapping("/{idProducto}")
    public ResponseEntity<StockResponse> obtener(@PathVariable Long idProducto) {
        return ResponseEntity.ok(stockService.obtener(idProducto));
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponse> entrada(@Valid @RequestBody EntradaRequest request) {
        return ResponseEntity.ok(stockService.registrarEntrada(request));
    }

    @PutMapping("/{idProducto}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponse> ajustar(@PathVariable Long idProducto, @Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.ok(stockService.ajustar(idProducto, request));
    }

    /** 200 { idVenta, expiraEn } · 409 con detalles ["idProducto: 7"] si algo no alcanza. */
    @PostMapping("/reservar")
    public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody ReservarRequest request) {
        return ResponseEntity.ok(stockService.reservar(request));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@Valid @RequestBody VentaRequest request) {
        stockService.confirmar(request.idVenta());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/liberar")
    public ResponseEntity<Void> liberar(@Valid @RequestBody VentaRequest request) {
        stockService.liberar(request.idVenta());
        return ResponseEntity.ok().build();
    }
}
