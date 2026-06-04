package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.dto.OrderEmailRequest;
import OCI.BabyShop.dto.OrderRequest;
import OCI.BabyShop.service.OrderEmailService;
import OCI.BabyShop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderEmailService orderEmailService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request, Authentication authentication) {
        String userEmail = authentication.getName();
        Order order = orderService.createOrder(userEmail, request.getProductQuantities(), request.getDiscountCode());
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendOrderEmail(@Valid @RequestBody OrderEmailRequest request) {
        orderEmailService.sendOrderEmail(request);
        return ResponseEntity.ok(Map.of("message", "Commande envoyée par email !"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok("Commande annulée avec succès");
    }
}
