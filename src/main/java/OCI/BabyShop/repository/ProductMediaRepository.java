package OCI.BabyShop.repository;

import OCI.BabyShop.domain.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {
    List<ProductMedia> findByProductId(UUID productId);

    @Modifying
    @Query("DELETE FROM ProductMedia pm WHERE pm.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}
