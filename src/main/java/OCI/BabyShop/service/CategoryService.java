package OCI.BabyShop.service;

import OCI.BabyShop.domain.Category;
import OCI.BabyShop.dto.CategoryTreeDto;
import OCI.BabyShop.repository.CategoryRepository;
import OCI.BabyShop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Retourne toutes les catégories (liste plate).
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Retourne une catégorie par son ID.
     */
    public Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
    }

    /**
     * Crée une nouvelle catégorie.
     */
    public Category createCategory(Category category) {
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie parente non trouvée"));
            if (parent.getParent() != null) {
                throw new IllegalArgumentException("Impossible d'ajouter plus de 2 niveaux de hiérarchie. Choisissez une catégorie racine comme parent.");
            }
            category.setParent(parent);
        }
        return categoryRepository.save(category);
    }

    /**
     * Modifie une catégorie existante.
     */
    public Category updateCategory(UUID id, Category updated) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (updated.getName() != null) {
            existing.setName(updated.getName());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getImageUrl() != null) {
            existing.setImageUrl(updated.getImageUrl());
        }
        if (updated.getParent() != null) {
            UUID parentId = updated.getParent().getId();
            if (parentId != null) {
                if (parentId.equals(id)) {
                    throw new IllegalArgumentException("Une catégorie ne peut pas être son propre parent");
                }
                Category parent = categoryRepository.findById(parentId)
                        .orElseThrow(() -> new IllegalArgumentException("Catégorie parente non trouvée"));
                if (parent.getParent() != null) {
                    throw new IllegalArgumentException("Impossible d'ajouter plus de 2 niveaux de hiérarchie. Choisissez une catégorie racine comme parent.");
                }
                existing.setParent(parent);
            } else {
                existing.setParent(null);
            }
        }

        return categoryRepository.save(existing);
    }

    /**
     * Supprime une catégorie si elle n'a ni produits ni sous-catégories.
     */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de supprimer une catégorie qui contient des sous-catégories");
        }

        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer une catégorie qui contient des produits (" + productCount + " produit(s))");
        }

        categoryRepository.delete(category);
    }

    /**
     * Retourne l'arborescence des catégories (racines avec leurs enfants).
     */
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
        return roots.stream()
                .map(this::toTreeDto)
                .toList();
    }

    private CategoryTreeDto toTreeDto(Category category) {
        List<CategoryTreeDto> children = category.getChildren().stream()
                .map(this::toTreeDto)
                .toList();
        return CategoryTreeDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(children)
                .build();
    }
}
