package pe.intiguard.catalogo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.catalogo.dto.ProductoResponse;
import pe.intiguard.catalogo.service.ProductoService;

import java.util.List;

/** PENDIENTE: paginación y filtros en GET; POST, PUT y DELETE solo para ADMIN. */
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }
}
