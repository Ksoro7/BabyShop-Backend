package OCI.BabyShop.service;

import OCI.BabyShop.domain.*;
import OCI.BabyShop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;
    private final PromoUsageRepository promoUsageRepository;
    private final NotificationService notificationService;
    private final CartService cartService;

    @Transactional
    public Order createOrder(String userEmail, Map<UUID, Integer> productQuantities, String discountCode) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .deliveryDate(LocalDate.now().plusDays(2))
                .build();

        BigDecimal subtotalAmount = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé"));

            if (product.getStockQty() < entry.getValue()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuffisant pour : " + product.getName());
            }

            product.setStockQty(product.getStockQty() - entry.getValue());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
            subtotalAmount = subtotalAmount.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(entry.getValue())
                    .unitPrice(product.getPrice())
                    .subtotal(lineTotal)
                    .build();

            order.getItems().add(item);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Discount appliedDiscount = null;

        if (discountCode != null && !discountCode.isBlank()) {
            appliedDiscount = discountRepository.findByCode(discountCode.toUpperCase().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code promo invalide"));

            if (!appliedDiscount.isActif()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code promo inactif");
            }

            LocalDate today = LocalDate.now();
            if (appliedDiscount.getDateDebut() != null && today.isBefore(appliedDiscount.getDateDebut())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code promo pas encore valide");
            }
            if (appliedDiscount.getDateExpiration() != null && today.isAfter(appliedDiscount.getDateExpiration())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code promo expiré");
            }

            if (appliedDiscount.getNbMaxUtilisations() != null
                    && appliedDiscount.getNbUtilisations() >= appliedDiscount.getNbMaxUtilisations()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code promo épuisé");
            }

            if (Boolean.TRUE.equals(appliedDiscount.getUsageUniqueParUser())) {
                boolean dejaUtilise = promoUsageRepository
                        .findByUserIdAndDiscountId(user.getId(), appliedDiscount.getId())
                        .isPresent();
                if (dejaUtilise) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous avez déjà utilisé ce code");
                }
            }

            if (appliedDiscount.getMontantMinAchat() != null
                    && subtotalAmount.compareTo(appliedDiscount.getMontantMinAchat()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Montant minimum de " + appliedDiscount.getMontantMinAchat() + " FCFA non atteint");
            }

            if (appliedDiscount.getType() == Discount.DiscountType.percentage) {
                discountAmount = subtotalAmount.multiply(appliedDiscount.getValeur())
                        .divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = appliedDiscount.getValeur().min(subtotalAmount);
            }

            appliedDiscount.setNbUtilisations(appliedDiscount.getNbUtilisations() + 1);
            discountRepository.save(appliedDiscount);
        }

        order.setDiscountApplied(discountAmount);
        order.setTotalAmount(subtotalAmount.subtract(discountAmount));

        Order savedOrder = orderRepository.save(order);

        if (appliedDiscount != null) {
            PromoUsage usage = PromoUsage.builder()
                    .user(user)
                    .discount(appliedDiscount)
                    .order(savedOrder)
                    .montantRemise(discountAmount)
                    .build();
            promoUsageRepository.save(usage);
        }

        cartService.clearCart(userEmail);

        notificationService.sendOrderNotifications(savedOrder);

        return savedOrder;
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Impossible d'annuler cette commande.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            p.setStockQty(p.getStockQty() + item.getQuantity());
            productRepository.save(p);
        }
        orderRepository.save(order);
    }
}
