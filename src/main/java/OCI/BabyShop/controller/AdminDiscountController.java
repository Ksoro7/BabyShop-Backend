package OCI.BabyShop.controller;

import OCI.BabyShop.dto.DiscountRequest;
import OCI.BabyShop.dto.DiscountResponse;
import OCI.BabyShop.dto.PromoUsageResponse;
import OCI.BabyShop.service.AdminDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/discounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiscountController {

    private final AdminDiscountService adminDiscountService;

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> getAllDiscounts() {
        return ResponseEntity.ok(adminDiscountService.getAllDiscounts());
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<PromoUsageResponse>> getUsages(@PathVariable UUID id) {
        return ResponseEntity.ok(adminDiscountService.getUsages(id));
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDiscountService.createDiscount(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> updateDiscount(@PathVariable UUID id,
                                                            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(adminDiscountService.updateDiscount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDiscount(@PathVariable UUID id) {
        adminDiscountService.deleteDiscount(id);
        return ResponseEntity.ok(Map.of("message", "Code promo supprimé avec succès"));
    }
}
