package OCI.BabyShop.controller;

import OCI.BabyShop.dto.AddCartArticleRequest;
import OCI.BabyShop.dto.AdminCartResponse;
import OCI.BabyShop.service.AdminCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/carts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCartController {

    private final AdminCartService adminCartService;

    @GetMapping
    public ResponseEntity<List<AdminCartResponse>> getAllCarts() {
        return ResponseEntity.ok(adminCartService.getAllCarts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCartResponse> getCart(@PathVariable UUID id) {
        return ResponseEntity.ok(adminCartService.getCart(id));
    }

    @PostMapping("/{id}/articles")
    public ResponseEntity<AdminCartResponse> addArticle(@PathVariable UUID id,
                                                         @Valid @RequestBody AddCartArticleRequest request) {
        return ResponseEntity.ok(adminCartService.addArticle(id, request));
    }

    @DeleteMapping("/{id}/articles/{articleId}")
    public ResponseEntity<Void> removeArticle(@PathVariable UUID id, @PathVariable UUID articleId) {
        adminCartService.removeArticle(id, articleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCart(@PathVariable UUID id) {
        adminCartService.deleteCart(id);
        return ResponseEntity.ok(Map.of("message", "Panier supprimé avec succès"));
    }
}
