package OCI.BabyShop.controller;

import OCI.BabyShop.dto.AdminOrderResponse;
import OCI.BabyShop.dto.StatusUpdateRequest;
import OCI.BabyShop.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>> getAllOrders() {
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOrderService.getOrder(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateStatus(@PathVariable UUID id,
                                                            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(adminOrderService.updateStatus(id, request.getStatus()));
    }
}
