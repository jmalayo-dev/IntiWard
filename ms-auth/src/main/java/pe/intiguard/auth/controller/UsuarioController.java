package pe.intiguard.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.service.UsuarioService;
import pe.intiguard.security.UsuarioActual;

import java.util.List;

/** PENDIENTE: POST, PUT y DELETE con @PreAuthorize("hasRole('ADMIN')"). */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    /** ADMIN ve a cualquiera; un CLIENTE solo a sí mismo. Sin token: 401. */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        if (!usuarioActual.esAdmin() && !id.equals(usuarioActual.id())) {
            throw new AccessDeniedException("Solo puede ver su propio usuario");
        }
        return ResponseEntity.ok(usuarioService.obtener(id));
    }
}
