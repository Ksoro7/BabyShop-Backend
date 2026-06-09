/**
 * @file    UserDiscountController.java
 * @change  [WELCOME-DISCOUNT] Nouveau endpoint GET /api/user/discount pour récupérer le code de bienvenue - 2026-06-08
 */

package OCI.BabyShop.controller;

import OCI.BabyShop.domain.User;
import OCI.BabyShop.domain.UserDiscount;
import OCI.BabyShop.repository.UserDiscountRepository;
import OCI.BabyShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserDiscountController {

    private final UserDiscountRepository userDiscountRepository;
    private final UserRepository userRepository;

    /**
     * Retourne le code de bienvenue de l'utilisateur connecté (s'il existe
     * et n'a pas encore été utilisé). Ne retourne rien si déjà utilisé.
     * <p>
     * Réponse : { discountCode, percentage, validUntil, isUsed }
     * ou 200 avec un body contenant null si aucun code dispo.
     */
    @GetMapping("/discount")
    public ResponseEntity<?> getMyDiscount(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        List<UserDiscount> discounts = userDiscountRepository.findByUserIdAndIsUsedFalse(user.getId());
        if (discounts.isEmpty()) {
            return ResponseEntity.ok(Map.of("discountCode", null, "percentage", null, "validUntil", null, "isUsed", true));
        }

        UserDiscount discount = discounts.get(0);
        return ResponseEntity.ok(Map.of(
                "discountCode", discount.getDiscountCode(),
                "percentage", discount.getPercentage(),
                "validUntil", discount.getValidUntil() != null ? discount.getValidUntil().toString() : null,
                "isUsed", discount.isUsed()
        ));
    }
}
