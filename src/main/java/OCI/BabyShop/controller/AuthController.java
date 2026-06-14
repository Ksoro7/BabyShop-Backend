package OCI.BabyShop.controller;

import OCI.BabyShop.dto.AuthResponse;
import OCI.BabyShop.dto.LoginRequest;
import OCI.BabyShop.dto.RegisterRequest;
import OCI.BabyShop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

/*
 * Refactored:
 * - register retourne HttpStatus.CREATED (201) au lieu de OK
 * - @Valid sur tous les @RequestBody
 * - Le traitement des erreurs est delegue a GlobalExceptionHandler
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le token de rafraîchissement est requis"));
        }
        authService.logout(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    // @change [PROD-READY] Endpoint mot de passe oublié - 2026-06-12
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'adresse email est requise"));
        }
        Map<String, String> response = authService.forgotPassword(email);
        return ResponseEntity.ok(response);
    }

    // @change [PROD-READY] Endpoint réinitialisation mot de passe - 2026-06-12
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le token est requis"));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nouveau mot de passe est requis"));
        }
        Map<String, String> response = authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }
}
