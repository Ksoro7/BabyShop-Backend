package OCI.BabyShop.controller;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.dto.OrderRequest;
import OCI.BabyShop.dto.UserOrderResponse;
import OCI.BabyShop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request, Authentication authentication) {
        String userEmail = authentication.getName();
        Order order = orderService.createOrder(userEmail, request.getProductQuantities(),
                request.getDiscountCode(), request.getPaymentMethod());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<UserOrderResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrders(authentication.getName()));
    }

    @GetMapping("/mine/{id}")
    public ResponseEntity<UserOrderResponse> getMyOrder(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrder(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok("Commande annulée avec succès");
    }
}
