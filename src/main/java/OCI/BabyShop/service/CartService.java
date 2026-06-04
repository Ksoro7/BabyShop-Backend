package OCI.BabyShop.service;

import OCI.BabyShop.domain.Cart;
import OCI.BabyShop.domain.CartItem;
import OCI.BabyShop.domain.Product;
import OCI.BabyShop.domain.User;
import OCI.BabyShop.dto.AddCartItemRequest;
import OCI.BabyShop.dto.CartItemResponse;
import OCI.BabyShop.dto.CartMergeRequest;
import OCI.BabyShop.dto.CartResponse;
import OCI.BabyShop.repository.CartItemRepository;
import OCI.BabyShop.repository.CartRepository;
import OCI.BabyShop.repository.ProductRepository;
import OCI.BabyShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String userEmail, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé"));

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + request.getQuantity()),
                        () -> {
                            CartItem newItem = CartItem.builder()
                                    .cart(cart)
                                    .product(product)
                                    .quantity(request.getQuantity())
                                    .build();
                            cart.getItems().add(newItem);
                        }
                );

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(String userEmail, UUID itemId, int quantity) {
        Cart cart = getOrCreateCart(userEmail);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article non trouvé dans le panier"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet article ne vous appartient pas");
        }

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String userEmail, UUID itemId) {
        Cart cart = getOrCreateCart(userEmail);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article non trouvé dans le panier"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet article ne vous appartient pas");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse mergeCart(String userEmail, CartMergeRequest request) {
        Cart cart = getOrCreateCart(userEmail);
        for (CartMergeRequest.MergeItem mergeItem : request.getItems()) {
            Product product = productRepository.findById(mergeItem.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produit non trouvé: " + mergeItem.getProductId()));

            cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(mergeItem.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> item.setQuantity(item.getQuantity() + mergeItem.getQuantity()),
                            () -> {
                                CartItem newItem = CartItem.builder()
                                        .cart(cart)
                                        .product(product)
                                        .quantity(mergeItem.getQuantity())
                                        .build();
                                cart.getItems().add(newItem);
                            }
                    );
        }
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productImage(item.getProduct().getMediaList().isEmpty()
                                ? null
                                : item.getProduct().getMediaList().get(0).getUrl())
                        .productPrice(item.getProduct().getPrice())
                        .quantity(item.getQuantity())
                        .addedAt(item.getAddedAt())
                        .build())
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
