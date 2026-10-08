package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.CambiarPasswordRequestDTO;
import com.chavez.store.api_rest.dto.request.LoginRequestDTO;
import com.chavez.store.api_rest.dto.request.RegistroUsuarioRequestDTO;
import com.chavez.store.api_rest.dto.response.JwtResponseDTO;
import com.chavez.store.api_rest.dto.response.UsuarioResponseDTO;
import com.chavez.store.api_rest.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroUsuarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(authService.usuarioActual(authentication.getName()));
    }

    @PutMapping("/password")
    public ResponseEntity<UsuarioResponseDTO> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequestDTO request) {
        return ResponseEntity.ok(authService.cambiarPassword(authentication.getName(), request));
    }
}