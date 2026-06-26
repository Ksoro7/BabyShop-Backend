package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Category;
import OCI.BabyShop.dto.CategoryTreeDto;
import OCI.BabyShop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Liste plate de toutes les catégories.
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Arborescence complète (racines avec leurs sous-catégories).
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    /**
     * Détail d'une catégorie par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

}
