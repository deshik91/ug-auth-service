package xyz.deshik91.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.deshik91.dto.request.LoginRequest;
import xyz.deshik91.dto.request.RefreshTokenRequest;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.dto.response.AuthResponse;
import xyz.deshik91.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // TODO: реализовать позже
        return ResponseEntity.ok("Логин пока не реализован");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        // TODO: реализовать позже
        return ResponseEntity.ok("Обновление токена пока не реализовано");
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token) {
        // TODO: реализовать позже
        return ResponseEntity.ok("Валидация пока не реализована");
    }
}