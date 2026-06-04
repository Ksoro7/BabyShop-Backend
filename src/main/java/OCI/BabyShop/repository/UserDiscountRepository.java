package OCI.BabyShop.repository;

import OCI.BabyShop.domain.UserDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserDiscountRepository extends JpaRepository<UserDiscount, UUID> {
    Optional<UserDiscount> findByDiscountCode(String discountCode);
    List<UserDiscount> findByUserIdAndIsUsedFalse(UUID userId);
}
