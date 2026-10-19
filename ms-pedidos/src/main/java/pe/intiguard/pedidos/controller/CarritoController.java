package pe.intiguard.pedidos.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.pedidos.dto.CantidadRequest;
import pe.intiguard.pedidos.dto.CarritoResponse;
import pe.intiguard.pedidos.dto.ItemCarritoRequest;
import pe.intiguard.pedidos.service.CarritoService;
import pe.intiguard.security.UsuarioActual;

/** Solo CLIENTE (un ADMIN recibe 403). El carrito es siempre el del usuario del token. */
@RestController
@RequestMapping("/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CarritoController {

    private final CarritoService carritoService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<CarritoResponse> ver() {
        return ResponseEntity.ok(carritoService.ver(usuarioActual.id()));
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregar(@Valid @RequestBody ItemCarritoRequest request) {
        int cantidad = request.cantidad() == null ? 1 : request.cantidad();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carritoService.agregar(usuarioActual.id(), request.idProducto(), cantidad));
    }

    @PutMapping("/items/{idProducto}")
    public ResponseEntity<CarritoResponse> cambiarCantidad(@PathVariable Long idProducto,
                                                           @Valid @RequestBody CantidadRequest request) {
        return ResponseEntity.ok(carritoService.cambiarCantidad(usuarioActual.id(), idProducto, request.cantidad()));
    }

    @DeleteMapping("/items/{idProducto}")
    public ResponseEntity<CarritoResponse> quitar(@PathVariable Long idProducto) {
        return ResponseEntity.ok(carritoService.quitar(usuarioActual.id(), idProducto));
    }
}
