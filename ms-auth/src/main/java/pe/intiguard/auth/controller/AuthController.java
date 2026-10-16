package pe.intiguard.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.intiguard.auth.dto.LoginRequest;
import pe.intiguard.auth.dto.LoginResponse;
import pe.intiguard.auth.service.AuthService;

/** Público (intiguard.security.rutas-publicas). PENDIENTE: POST /auth/registro. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 200 con el token · 400 datos inválidos · 401 credenciales inválidas · 423 cuenta bloqueada. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
