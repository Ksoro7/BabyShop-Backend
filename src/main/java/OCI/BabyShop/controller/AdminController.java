package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Product;
import OCI.BabyShop.dto.ProductRequest;
import OCI.BabyShop.dto.ProductResponseDto;
import OCI.BabyShop.service.MediaUploadService;
import OCI.BabyShop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final MediaUploadService mediaUploadService;

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
                                               @RequestParam("file") MultipartFile file,
                                               HttpSession session) throws IOException {
        Integer uploadCount = (Integer) session.getAttribute("UPLOAD_COUNT");
        if (uploadCount == null) {
            uploadCount = 0;
        }
        if (uploadCount >= 3) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(null);
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String contentType = file.getContentType();
        if (contentType == null || !TYPES_AUTORISES.contains(contentType)) {
            return ResponseEntity.badRequest()
                    .body(null);
        }
        String fileUrl = mediaUploadService.upload(file, "uploads/products/");
        Product product = productService.addMedia(id, fileUrl, "IMAGE");
        session.setAttribute("UPLOAD_COUNT", uploadCount + 1);
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
