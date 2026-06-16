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
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final JavaMailSender mailSender;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
        envoyerEmailReset(user.getEmail(), resetLink);

        return Map.of("message", responseMessage);
    }

    private void envoyerEmailReset(String email, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Réinitialisation de votre mot de passe RaBiShop");

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"></head>
                    <body style="margin:0;padding:0;background:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;">
                        <table style="max-width:600px;margin:20px auto;background:#fff;border-radius:8px;overflow:hidden;">
                            <tr>
                                <td style="background:linear-gradient(135deg,#e91e63,#ad1457);padding:30px;text-align:center;">
                                    <h1 style="color:#fff;margin:0;font-size:24px;">RaBiShop</h1>
                                    <p style="color:rgba(255,255,255,.85);margin:5px 0 0;">Réinitialisation de mot de passe</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:30px;">
                                    <p style="font-size:16px;margin:0 0 20px;">Vous avez demandé une réinitialisation de votre mot de passe.</p>
                                    <p style="margin:0 0 20px;">
                                        <a href="%s"
                                           style="display:inline-block;background:#e91e63;color:#fff;padding:12px 30px;border-radius:6px;text-decoration:none;font-size:16px;">
                                           Réinitialiser mon mot de passe
                                        </a>
                                    </p>
                                    <p style="color:#888;font-size:13px;">Ce lien expire dans 1 heure.</p>
                                    <p style="color:#888;font-size:13px;">Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>
                                    <p style="color:#888;font-size:13px;">Lien direct : <a href="%s">%s</a></p>
                                </td>
                            </tr>
                            <tr>
                                <td style="background:#f9f9f9;padding:20px;text-align:center;font-size:12px;color:#999;">
                                    L'équipe RaBiShop
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """.formatted(resetLink, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé à {}", email);
        } catch (Exception e) {
            log.warn("SMTP indisponible ({}), lien de réinitialisation (destinataire: {}): {}",
                    e.getClass().getSimpleName(), email, resetLink);
        }
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
