package pe.intiguard.catalogo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.catalogo.dto.NombreActivoResponse;
import pe.intiguard.catalogo.service.MarcaCategoriaService;
import pe.intiguard.security.UsuarioActual;

import java.util.List;
import java.util.Map;

/** GET público (el público recibe solo las activas; el ADMIN, todas). Crear y editar: solo ADMIN. */
@RestController
@RequiredArgsConstructor
public class MarcaCategoriaController {

    private final MarcaCategoriaService service;
    private final UsuarioActual usuarioActual;

    @GetMapping("/marcas")
    public ResponseEntity<List<NombreActivoResponse>> marcas() {
        return ResponseEntity.ok(service.listarMarcas(usuarioActual.esAdmin()));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<NombreActivoResponse>> categorias() {
        return ResponseEntity.ok(service.listarCategorias(usuarioActual.esAdmin()));
    }

    @PostMapping("/marcas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NombreActivoResponse> crearMarca(@RequestBody Map<String, Object> datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearMarca(datos));
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NombreActivoResponse> crearCategoria(@RequestBody Map<String, Object> datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCategoria(datos));
    }

    @PutMapping("/marcas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NombreActivoResponse> actualizarMarca(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return ResponseEntity.ok(service.actualizarMarca(id, datos));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NombreActivoResponse> actualizarCategoria(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return ResponseEntity.ok(service.actualizarCategoria(id, datos));
    }
}
