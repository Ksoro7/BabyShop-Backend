package OCI.BabyShop.service;

import OCI.BabyShop.domain.Discount;
import OCI.BabyShop.dto.DiscountRequest;
import OCI.BabyShop.dto.DiscountResponse;
import OCI.BabyShop.dto.PromoUsageResponse;
import OCI.BabyShop.repository.DiscountRepository;
import OCI.BabyShop.repository.PromoUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDiscountService {

    private final DiscountRepository discountRepository;
    private final PromoUsageRepository promoUsageRepository;

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromoUsageResponse> getUsages(UUID discountId) {
        return promoUsageRepository.findByDiscountId(discountId).stream()
                .map(u -> PromoUsageResponse.builder()
                        .id(u.getId())
                        .userEmail(u.getUser().getEmail())
                        .discountId(u.getDiscount().getId())
                        .discountCode(u.getDiscount().getCode())
                        .orderId(u.getOrder() != null ? u.getOrder().getId() : null)
                        .montantRemise(u.getMontantRemise())
                        .usedAt(u.getUsedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public DiscountResponse createDiscount(DiscountRequest request) {
        Discount discount = Discount.builder()
                .code(request.getCode().toUpperCase().trim())
                .type(Discount.DiscountType.valueOf(request.getType()))
                .valeur(request.getValeur())
                .montantMinAchat(request.getMontantMinAchat())
                .dateDebut(request.getDateDebut())
                .dateExpiration(request.getDateExpiration())
                .nbMaxUtilisations(request.getNbMaxUtilisations())
                .usageUniqueParUser(request.getUsageUniqueParUser())
                .actif(request.isActif())
                .build();
        discountRepository.save(discount);
        return toResponse(discount);
    }

    @Transactional
    public DiscountResponse updateDiscount(UUID id, DiscountRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code promo non trouvé"));

        discount.setCode(request.getCode().toUpperCase().trim());
        discount.setType(Discount.DiscountType.valueOf(request.getType()));
        discount.setValeur(request.getValeur());
        discount.setMontantMinAchat(request.getMontantMinAchat());
        discount.setDateDebut(request.getDateDebut());
        discount.setDateExpiration(request.getDateExpiration());
        discount.setNbMaxUtilisations(request.getNbMaxUtilisations());
        discount.setUsageUniqueParUser(request.getUsageUniqueParUser());
        discount.setActif(request.isActif());

        discountRepository.save(discount);
        return toResponse(discount);
    }

    @Transactional
    public void deleteDiscount(UUID id) {
        discountRepository.deleteById(id);
    }

    private DiscountResponse toResponse(Discount d) {
        return DiscountResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .type(d.getType().name())
                .valeur(d.getValeur())
                .montantMinAchat(d.getMontantMinAchat())
                .dateDebut(d.getDateDebut())
                .dateExpiration(d.getDateExpiration())
                .nbMaxUtilisations(d.getNbMaxUtilisations())
                .nbUtilisations(d.getNbUtilisations())
                .usageUniqueParUser(d.getUsageUniqueParUser())
                .actif(d.isActif())
                .build();
    }
}
