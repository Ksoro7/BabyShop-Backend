package OCI.BabyShop.service;

import OCI.BabyShop.domain.*;
import OCI.BabyShop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserDiscountRepository userDiscountRepository;

    @Autowired
    private PromoUsageRepository promoUsageRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderNotificationRepository orderNotificationRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;
    private Product testProduct2;
    private static final String USER_EMAIL = "orderuser@test.com";

    @BeforeEach
    void setUp() {
        orderNotificationRepository.deleteAll();
        promoUsageRepository.deleteAll();
        userDiscountRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        productMediaRepository.deleteAll();
        discountRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email(USER_EMAIL)
                .passwordHash(passwordEncoder.encode("Password1!"))
                .firstName("Commande")
                .lastName("Test")
                .role(Role.USER)
                .isActive(true)
                .build());

        Category cat = categoryRepository.save(Category.builder()
                .name("TestCat")
                .build());

        testProduct = productRepository.save(Product.builder()
                .sku("ORDTEST001")
                .name("Produit Test 1")
                .price(new BigDecimal("5000"))
                .stockQty(10)
                .category(cat)
                .isActive(true)
                .build());

        testProduct2 = productRepository.save(Product.builder()
                .sku("ORDTEST002")
                .name("Produit Test 2")
                .price(new BigDecimal("3000"))
                .stockQty(5)
                .category(cat)
                .isActive(true)
                .build());
    }

    @Test
    void createOrder_shouldDecrementStock() {
        Map<UUID, Integer> items = Map.of(testProduct.getId(), 3);

        orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(7, updated.getStockQty());
    }

    @Test
    void createOrder_shouldFailOnInsufficientStock() {
        Map<UUID, Integer> items = Map.of(testProduct.getId(), 20);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address"));
        assertTrue(ex.getReason().contains("Stock insuffisant"));
    }

    @Test
    void createOrder_shouldDecrementMultipleProducts() {
        Map<UUID, Integer> items = Map.of(
                testProduct.getId(), 2,
                testProduct2.getId(), 1
        );

        orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        assertEquals(8, productRepository.findById(testProduct.getId()).orElseThrow().getStockQty());
        assertEquals(4, productRepository.findById(testProduct2.getId()).orElseThrow().getStockQty());
    }

    @Test
    void createOrder_shouldApplyPercentageDiscount() {
        Discount promo = discountRepository.save(Discount.builder()
                .code("TESTPROMO10")
                .type(Discount.DiscountType.percentage)
                .valeur(new BigDecimal("10"))
                .montantMinAchat(BigDecimal.ZERO)
                .usageUniqueParUser(false)
                .actif(true)
                .build());

        Map<UUID, Integer> items = Map.of(testProduct.getId(), 2);
        BigDecimal expectedTotal = new BigDecimal("10000").subtract(new BigDecimal("1000"));

        Order order = orderService.createOrder(USER_EMAIL, items, "TESTPROMO10", "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        assertEquals(0, new BigDecimal("9000").compareTo(order.getTotalAmount()));
        assertNotNull(order.getDiscountApplied());
        assertEquals(1, discountRepository.findById(promo.getId()).orElseThrow().getNbUtilisations());
    }

    @Test
    void cancelOrder_shouldRestoreStock() {
        Map<UUID, Integer> items = Map.of(testProduct.getId(), 4);
        Order order = orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        orderService.cancelOrder(order.getId(), USER_EMAIL);

        assertEquals(10, productRepository.findById(testProduct.getId()).orElseThrow().getStockQty());
        assertEquals(OrderStatus.CANCELLED,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelOrder_shouldFailForWrongOwner() {
        User otherUser = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .firstName("Autre")
                .lastName("User")
                .role(Role.USER)
                .isActive(true)
                .build());

        Map<UUID, Integer> items = Map.of(testProduct.getId(), 2);
        Order order = orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(order.getId(), "other@test.com"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void createOrder_shouldFailForNonExistentProduct() {
        Map<UUID, Integer> items = Map.of(UUID.randomUUID(), 1);

        assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address"));
    }

    @Test
    void createOrder_shouldUseAllStockThenCancel() {
        Map<UUID, Integer> items = Map.of(testProduct2.getId(), 5);
        Order order = orderService.createOrder(USER_EMAIL, items, null, "WHATSAPP", "Test User", "+2250101020203", "Test Address");

        assertEquals(0, productRepository.findById(testProduct2.getId()).orElseThrow().getStockQty());

        orderService.cancelOrder(order.getId(), USER_EMAIL);

        assertEquals(5, productRepository.findById(testProduct2.getId()).orElseThrow().getStockQty());
    }
}
