package OCI.BabyShop.repository;

import OCI.BabyShop.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Retourne toutes les catégories racines (sans parent).
     */
    List<Category> findByParentIsNull();

    /**
     * Retourne les sous-catégories d'une catégorie donnée.
     */
    List<Category> findByParentId(UUID parentId);

    /**
     * Recherche une catégorie par son nom exact.
     */
    Optional<Category> findByName(String name);
}
