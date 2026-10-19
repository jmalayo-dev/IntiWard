package pe.intiguard.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.auth.dto.RegistroRequest;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.service.UsuarioService;
import pe.intiguard.security.UsuarioActual;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioActual usuarioActual;

    /** Todos, incluidos los inactivos. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    /** El usuario del token. La ruta literal "/me" gana sobre "/{id}". */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> yo() {
        return ResponseEntity.ok(usuarioService.obtener(usuarioActual.id()));
    }

    /**
     * Solo los campos que cambian. Se recibe como Map para distinguir "no vino fotoUrl"
     * de "fotoUrl: null" (quitar la foto).
     */
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizarYo(@RequestBody Map<String, Object> cambios) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(usuarioActual.id(), cambios));
    }

    /** ADMIN ve a cualquiera; un CLIENTE solo a sí mismo. Sin token: 401. */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        if (!usuarioActual.esAdmin() && !id.equals(usuarioActual.id())) {
            throw new AccessDeniedException("Solo puede ver su propio usuario");
        }
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearDesdePanel(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> cambios) {
        return ResponseEntity.ok(usuarioService.actualizar(id, cambios));
    }

    /** Desactiva la cuenta (nunca se borra). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
