package OCI.BabyShop.repository;

import OCI.BabyShop.domain.PromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, UUID> {
    Optional<PromoUsage> findByUserIdAndDiscountId(UUID userId, UUID discountId);
    List<PromoUsage> findByDiscountId(UUID discountId);
    long countByDiscountId(UUID discountId);
}
