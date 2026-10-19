package pe.intiguard.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.intiguard.auth.dto.LoginRequest;
import pe.intiguard.auth.dto.LoginResponse;
import pe.intiguard.auth.dto.RegistroRequest;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.service.AuthService;
import pe.intiguard.auth.service.UsuarioService;

/** Público (intiguard.security.rutas-publicas). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    /** 200 con el token · 400 datos inválidos · 401 credenciales inválidas · 423 cuenta bloqueada. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** 201 con el usuario (siempre CLIENTE) · 400 datos inválidos · 409 correo ya registrado. */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }
}
