package OCI.BabyShop.controller;

import OCI.BabyShop.dto.AddCartItemRequest;
import OCI.BabyShop.dto.CartMergeRequest;
import OCI.BabyShop.dto.CartResponse;
import OCI.BabyShop.dto.UpdateCartItemRequest;
import OCI.BabyShop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request,
                                                Authentication authentication) {
        return ResponseEntity.ok(cartService.addItem(authentication.getName(), request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateCartItemRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(cartService.updateItemQuantity(authentication.getName(), id, request.getQuantity()));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable UUID id,
                                                   Authentication authentication) {
        return ResponseEntity.ok(cartService.removeItem(authentication.getName(), id));
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(@Valid @RequestBody CartMergeRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(cartService.mergeCart(authentication.getName(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
