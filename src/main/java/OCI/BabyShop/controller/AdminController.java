package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Product;
import OCI.BabyShop.dto.ProductRequest;
import OCI.BabyShop.dto.ProductResponseDto;
import OCI.BabyShop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> TYPES_AUTORISES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "image/gif", "image/avif", "image/svg+xml"
    );

    private final ProductService productService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        Product product = productService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> uploadMedia(@PathVariable UUID id,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String contentType = file.getContentType();
        if (contentType == null || !TYPES_AUTORISES.contains(contentType)) {
            return ResponseEntity.badRequest()
                    .body(null); // géré par GlobalExceptionHandler
        }
        String uploadDir = "uploads/products/";
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Path.of(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        String fileUrl = baseUrl + "/uploads/products/" + fileName;
        Product product = productService.addMedia(id, fileUrl, "IMAGE");
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Produit supprimé avec succès"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, String>> restoreProduct(@PathVariable UUID id) {
        productService.restoreProduct(id);
        return ResponseEntity.ok(Map.of("message", "Produit restauré avec succès"));
    }
}
