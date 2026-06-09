package OCI.BabyShop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
/**
 * @change [WELCOME-DISCOUNT] Ajout welcomeDiscountCode nullable pour le code de bienvenue - 2026-06-08
 */
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String role;
    private String email;
    private String welcomeDiscountCode;
}
