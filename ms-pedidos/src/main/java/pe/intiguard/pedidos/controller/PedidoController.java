package pe.intiguard.pedidos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.service.PedidoService;

import java.util.List;

/** PENDIENTE: POST /pedidos/checkout y CarritoController (/carrito/items). */
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }
}
