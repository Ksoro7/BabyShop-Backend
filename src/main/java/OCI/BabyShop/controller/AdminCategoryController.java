package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Category;
import OCI.BabyShop.dto.CategoryRequest;
import OCI.BabyShop.service.CategoryService;
import OCI.BabyShop.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final Set<String> TYPES_AUTORISES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "image/gif", "image/avif", "image/svg+xml"
    );

    private final CategoryService categoryService;
    private final MediaUploadService mediaUploadService;

    /**
     * Crée une nouvelle catégorie.
     */
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        if (request.getParentId() != null) {
            category.setParent(Category.builder().id(request.getParentId()).build());
        }
        Category created = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Modifie une catégorie existante (nom, description, parent).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable UUID id, @RequestBody CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        if (request.getParentId() != null) {
            category.setParent(Category.builder().id(request.getParentId()).build());
        } else {
            // null explicite → supprimer le parent (remonter en racine)
            category.setParent(Category.builder().id(null).build());
        }
        Category updated = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une catégorie (uniquement si sans produits et sans sous-catégories).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Catégorie supprimée avec succès"));
    }

    /**
     * Upload d'une image pour une catégorie. Remplace l'image existante.
     * Accepte les types MIME : JPEG, PNG, WebP, GIF, AVIF, SVG.
     *
     * @param id   identifiant de la catégorie
     * @param file fichier image à uploader
     * @return la catégorie mise à jour avec sa nouvelle imageUrl
     */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Category> uploadImage(@PathVariable UUID id,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String contentType = file.getContentType();
        if (contentType == null || !TYPES_AUTORISES.contains(contentType)) {
            return ResponseEntity.badRequest().build();
        }
        String fileUrl = mediaUploadService.upload(file, "uploads/categories/");
        Category category = Category.builder().imageUrl(fileUrl).build();
        Category updated = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(updated);
    }
}
