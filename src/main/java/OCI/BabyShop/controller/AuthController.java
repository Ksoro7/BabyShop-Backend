package OCI.BabyShop.controller;

import OCI.BabyShop.dto.AuthResponse;
import OCI.BabyShop.dto.LoginRequest;
import OCI.BabyShop.dto.RegisterRequest;
import OCI.BabyShop.service.AuthService;
// Import conservé uniquement pour la lecture des cookies (extractRefreshTokenFromCookie)
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) Map<String, String> body,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        String refreshToken = null;
        if (body != null) {
            refreshToken = body.get("refreshToken");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractRefreshTokenFromCookie(request);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

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
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) Map<String, String> body,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        String refreshToken = null;
        if (body != null) {
            refreshToken = body.get("refreshToken");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractRefreshTokenFromCookie(request);
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthResponse authResponse = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
