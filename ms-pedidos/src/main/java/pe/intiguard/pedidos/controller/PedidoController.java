package pe.intiguard.pedidos.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.pedidos.dto.CheckoutRequest;
import pe.intiguard.pedidos.dto.VentaResponse;
import pe.intiguard.pedidos.service.CheckoutService;
import pe.intiguard.pedidos.service.PedidoService;
import pe.intiguard.security.UsuarioActual;

import java.util.List;

/** Todas exigen sesión. El usuario sale siempre del token, nunca del cuerpo. */
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final CheckoutService checkoutService;
    private final UsuarioActual usuarioActual;

    /** ADMIN: todas. CLIENTE: las suyas. Con detalles. */
    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }

    /**
     * 201 venta CONFIRMADA · 400 datos o carrito vacío · 409 stock insuficiente (detalles: ["idProducto: 7"])
     * · 503 comprobantes o inventario caídos (la reserva se libera y la venta queda ANULADA).
     * El token se reenvía a ms-inventario y ms-comprobantes.
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<VentaResponse> checkout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
                                                  @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(usuarioActual.id(), token, request));
    }
}
