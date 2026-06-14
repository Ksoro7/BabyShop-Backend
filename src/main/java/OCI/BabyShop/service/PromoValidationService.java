package OCI.BabyShop.service;

import OCI.BabyShop.domain.Discount;
import OCI.BabyShop.domain.PromoUsage;
import OCI.BabyShop.domain.UserDiscount;
import OCI.BabyShop.dto.PromoValidationRequest;
import OCI.BabyShop.dto.PromoValidationResponse;
import OCI.BabyShop.repository.DiscountRepository;
import OCI.BabyShop.repository.PromoUsageRepository;
import OCI.BabyShop.repository.UserDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromoValidationService {

    private final DiscountRepository discountRepository;
    private final PromoUsageRepository promoUsageRepository;
    private final UserDiscountRepository userDiscountRepository;

    public PromoValidationResponse valider(PromoValidationRequest req) {
        String code = req.getCode().toUpperCase().trim();
        BigDecimal montantPanier = req.getMontantPanier() != null ? req.getMontantPanier() : BigDecimal.ZERO;

        Discount discount = discountRepository.findByCode(code).orElse(null);

        if (discount == null) {
            UserDiscount userDisc = userDiscountRepository.findByDiscountCode(code).orElse(null);
            if (userDisc != null) {
                if (userDisc.isUsed()) {
                    return invalide("Ce code promo a déjà été utilisé.");
                }
                if (userDisc.getValidUntil() != null && LocalDateTime.now().isAfter(userDisc.getValidUntil())) {
                    return invalide("Ce code promo a expiré.");
                }
                BigDecimal remise = montantPanier.multiply(userDisc.getPercentage()).divide(BigDecimal.valueOf(100));
                return PromoValidationResponse.builder()
                        .valide(true)
                        .remise(remise)
                        .type("percentage")
                        .message("Code " + code + " appliqué : -" + userDisc.getPercentage() + "%")
                        .build();
            }
            return invalide("Code promo invalide.");
        }

        if (!discount.isActif()) {
            return invalide("Ce code promo n'est plus actif.");
        }

        LocalDate today = LocalDate.now();
        if (discount.getDateDebut() != null && today.isBefore(discount.getDateDebut())) {
            return invalide("Ce code promo n'est pas encore valide.");
        }
        if (discount.getDateExpiration() != null && today.isAfter(discount.getDateExpiration())) {
            return invalide("Ce code promo a expiré.");
        }

        if (discount.getNbMaxUtilisations() != null && discount.getNbUtilisations() >= discount.getNbMaxUtilisations()) {
            return invalide("Ce code promo a atteint sa limite d'utilisations.");
        }

        if (Boolean.TRUE.equals(discount.getUsageUniqueParUser()) && req.getUserId() != null) {
            boolean dejaUtilise = promoUsageRepository
                    .findByUserIdAndDiscountId(req.getUserId(), discount.getId())
                    .isPresent();
            if (dejaUtilise) {
                return invalide("Vous avez déjà utilisé ce code promo.");
            }
        }

        if (discount.getMontantMinAchat() != null && montantPanier.compareTo(discount.getMontantMinAchat()) < 0) {
            return invalide("Montant minimum d'achat de " + discount.getMontantMinAchat() + " FCFA non atteint.");
        }

        BigDecimal remise;
        String typeLabel;
        if (discount.getType() == Discount.DiscountType.percentage) {
            remise = montantPanier.multiply(discount.getValeur()).divide(BigDecimal.valueOf(100));
            typeLabel = "percentage";
        } else {
            remise = discount.getValeur().min(montantPanier);
            typeLabel = "fixed";
        }

        return PromoValidationResponse.builder()
                .valide(true)
                .remise(remise)
                .type(typeLabel)
                .message("Code " + code + " appliqué : -" + discount.getValeur()
                        + (discount.getType() == Discount.DiscountType.percentage ? "%" : " FCFA"))
                .build();
    }

    private PromoValidationResponse invalide(String message) {
        return PromoValidationResponse.builder()
                .valide(false)
                .remise(BigDecimal.ZERO)
                .type("")
                .message(message)
                .build();
    }
}
