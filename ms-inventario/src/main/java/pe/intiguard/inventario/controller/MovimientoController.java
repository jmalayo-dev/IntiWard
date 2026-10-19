package pe.intiguard.inventario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.intiguard.inventario.dto.MovimientoResponse;
import pe.intiguard.inventario.service.StockService;

import java.util.List;

/** Historial auditable del stock (vista 5.6). Solo ADMIN. */
@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MovimientoResponse>> listar(@RequestParam(required = false) Long idProducto) {
        return ResponseEntity.ok(stockService.movimientos(idProducto));
    }
}
