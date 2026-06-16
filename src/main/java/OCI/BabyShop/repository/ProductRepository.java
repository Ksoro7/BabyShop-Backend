package OCI.BabyShop.repository;

import OCI.BabyShop.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQty > 0 AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQty > 0")
    Page<Product> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true AND p.stockQty > 0")
    java.util.Optional<Product> findActiveById(@Param("id") UUID id);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    long countByCategoryId(UUID categoryId);

    /**
     * Produits actifs et en stock d'une catégorie donnée.
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true AND p.stockQty > 0")
    Page<Product> findActiveByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Recherche par mot-clé dans une catégorie spécifique (produits actifs et en stock).
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true AND p.stockQty > 0 AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchActiveByKeywordAndCategoryId(@Param("keyword") String keyword, @Param("categoryId") UUID categoryId, Pageable pageable);

    long countByStockQty(int stockQty);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQty > 0 AND p.stockQty <= :threshold")
    long countStockFaible(@Param("threshold") int threshold);

    List<Product> findTop5ByOrderByStockQtyAsc(Pageable pageable);

    @Modifying
    @Query(value = "UPDATE products SET version = 0 WHERE version IS NULL", nativeQuery = true)
    void fixNullVersion();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findByIdWithLock(@Param("id") UUID id);
}
