package pe.intiguard.comprobantes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.service.ComprobanteService;

import java.util.List;

/** PENDIENTE: POST /comprobantes (interno, lo llama ms-pedidos) y GET /comprobantes/{id}/pdf. */
@RestController
@RequestMapping("/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping
    public ResponseEntity<List<ComprobanteResponse>> listar() {
        return ResponseEntity.ok(comprobanteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(comprobanteService.obtener(id));
    }
}
