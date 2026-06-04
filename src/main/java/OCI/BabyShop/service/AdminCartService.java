package OCI.BabyShop.service;

import OCI.BabyShop.domain.Cart;
import OCI.BabyShop.domain.CartItem;
import OCI.BabyShop.domain.Product;
import OCI.BabyShop.dto.AddCartArticleRequest;
import OCI.BabyShop.dto.AdminCartResponse;
import OCI.BabyShop.repository.CartRepository;
import OCI.BabyShop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<AdminCartResponse> getAllCarts() {
        return cartRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(AdminCartResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminCartResponse getCart(UUID id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé"));
        return toResponse(cart);
    }

    @Transactional
    public AdminCartResponse addArticle(UUID cartId, AddCartArticleRequest request) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.getQuantity())
                .build();
        cart.getItems().add(item);
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void removeArticle(UUID cartId, UUID articleId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Panier non trouvé"));
        cart.getItems().removeIf(item -> item.getId().equals(articleId));
        cartRepository.save(cart);
    }

    @Transactional
    public void deleteCart(UUID id) {
        cartRepository.deleteById(id);
    }

    private AdminCartResponse toResponse(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String customerName = cart.getUser().getFirstName() + " " + cart.getUser().getLastName();
        if (customerName.isBlank()) customerName = cart.getUser().getEmail();

        List<AdminCartResponse.CartArticleDto> articleDtos = cart.getItems().stream()
                .map(item -> {
                    String imageUrl = item.getProduct().getMediaList().isEmpty() ? null
                            : item.getProduct().getMediaList().get(0).getUrl();
                    return AdminCartResponse.CartArticleDto.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .imageUrl(imageUrl)
                            .price(item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminCartResponse.builder()
                .id(cart.getId())
                .customerName(customerName)
                .customerEmail(cart.getUser().getEmail())
                .articles(articleDtos)
                .total(total)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
