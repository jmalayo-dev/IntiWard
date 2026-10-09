package pe.intiguard.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.service.UsuarioService;

import java.util.List;

/** PENDIENTE: POST, PUT y DELETE con @PreAuthorize("hasRole('ADMIN')"). */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }
}
