package OCI.BabyShop.service;

import OCI.BabyShop.domain.RefreshToken;
import OCI.BabyShop.domain.Role;
import OCI.BabyShop.domain.User;
import OCI.BabyShop.domain.UserDiscount;
import OCI.BabyShop.dto.AuthResponse;
import OCI.BabyShop.dto.LoginRequest;
import OCI.BabyShop.dto.RegisterRequest;
import OCI.BabyShop.repository.RefreshTokenRepository;
import OCI.BabyShop.repository.UserDiscountRepository;
import OCI.BabyShop.repository.UserRepository;
import OCI.BabyShop.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/*
 * Refactored:
 * - register() valide que le password n'est pas null/vide avant d'encoder
 * - login() supprime le double chargement : plus de UserDetailsService,
 *   on charge l'utilisateur une seule fois depuis userRepository
 * - Tous les IllegalArgumentException remplaces par ResponseStatusException
 *   avec le bon HttpStatus
 * - Import IMessage supprime
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserDiscountRepository userDiscountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_VALIDITY_HOURS = 1;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe ne peut pas être vide");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .isActive(true)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        String discountCode = "WELCOME10-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        creerDiscount(user, discountCode);

        log.info("Nouvel utilisateur enregistré : {}", request.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .email(user.getEmail())
                .welcomeDiscountCode(discountCode)
                .build();
    }

    private void creerDiscount(User user, String discountCode) {
        UserDiscount discount = UserDiscount.builder()
                .user(user)
                .discountCode(discountCode)
                .percentage(new BigDecimal("10.00"))
                .validUntil(LocalDateTime.now().plusDays(30))
                .isUsed(false)
                .build();
        userDiscountRepository.save(discount);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de rafraîchissement invalide"));
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

    @Transactional
    public Map<String, String> forgotPassword(String email) {
        // SÉCURITÉ : Réponse identique que l'email existe ou non (évite l'énumération d'emails)
        String responseMessage = "Si cet email existe, vous recevrez un lien de réinitialisation.";

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Tentative de réinitialisation pour email inconnu: {}", email);
            return Map.of("message", responseMessage);
        }

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userRepository.save(user);

        String resetLink = appBaseUrl + "/reset-password?token=" + token;
        // SÉCURITÉ : Email désactivé pour l'instant, on logge le lien
        log.info("Reset password link: {}", resetLink);

        return Map.of("message", responseMessage);
    }

    @Transactional
    public Map<String, String> resetPassword(String token, String newPassword) {
        // RÈGLE MÉTIER : Validation du mot de passe
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins 8 caractères");
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins une majuscule");
        }
        if (!newPassword.matches(".*[0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins un chiffre");
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins un caractère spécial");
        }

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token de réinitialisation invalide"));

        if (user.getResetPasswordTokenExpiry() == null
                || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Token de réinitialisation expiré");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        log.info("Mot de passe réinitialisé pour: {}", user.getEmail());

        return Map.of("message", "Mot de passe réinitialisé avec succès");
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de rafraîchissement invalide"));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement expiré ou révoqué");
        }

        User user = storedToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }
}
