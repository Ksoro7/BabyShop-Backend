package OCI.BabyShop.service;

import OCI.BabyShop.domain.Category;
import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.Product;
import OCI.BabyShop.domain.ProductMedia;
import static OCI.BabyShop.domain.Product.LOW_STOCK_THRESHOLD;
import OCI.BabyShop.dto.DashboardStatsResponse;
import OCI.BabyShop.dto.ProductRequest;
import OCI.BabyShop.dto.ProductResponseDto;
import OCI.BabyShop.repository.CartItemRepository;
import OCI.BabyShop.repository.CategoryRepository;
import OCI.BabyShop.repository.OrderRepository;
import OCI.BabyShop.repository.ProductMediaRepository;
import OCI.BabyShop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMediaRepository productMediaRepository;

    /**
     * Recherche les produits actifs avec filtres optionnels par mot-clé et/ou catégorie.
     * @param keyword    mot-clé pour la recherche (null ou vide = pas de filtre texte)
     * @param categoryId ID de catégorie (null = pas de filtre catégorie)
     * @param pageable   pagination / tri
     * @return page de produits actifs et en stock
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String keyword, UUID categoryId, Pageable pageable) {
        Page<Product> products;
        if (categoryId != null) {
            if (keyword != null && !keyword.isBlank()) {
                products = productRepository.searchActiveByKeywordAndCategoryId(keyword, categoryId, pageable);
            } else {
                products = productRepository.findActiveByCategoryId(categoryId, pageable);
            }
        } else if (keyword == null || keyword.isBlank()) {
            products = productRepository.findAllActive(pageable);
        } else {
            products = productRepository.searchByKeyword(keyword, pageable);
        }
        return products.map(this::toResponseDto);
    }

    private ProductResponseDto toResponseDto(Product product) {
        List<ProductResponseDto.MediaInfo> mediaList = product.getMediaList().stream()
                .map(m -> ProductResponseDto.MediaInfo.builder()
                        .id(m.getId())
                        .url(m.getUrl())
                        .type(m.getType().name())
                        .order(m.getOrder())
                        .build())
                .toList();
        String imageUrl = mediaList.isEmpty() ? null : mediaList.get(0).getUrl();
        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQty(product.getStockQty())
                .stockStatus(product.getStockStatus().name())
                .category(new ProductResponseDto.CategoryInfo(product.getCategory().getId(), product.getCategory().getName()))
                .mediaList(mediaList)
                .imageUrl(imageUrl)
                .isActive(product.isActive())
                .createdAt(product.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    public Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getActiveProduct(UUID id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé ou indisponible"));
        return toResponseDto(product);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQty(request.getStockQty())
                .category(category)
                .isActive(request.isActive())
                .build();

        if (request.getMediaList() != null) {
            product.setMediaList(request.getMediaList().stream().map(m -> {
                ProductMedia media = ProductMedia.builder()
                        .product(product)
                        .url(m.getUrl())
                        .type(ProductMedia.MediaType.valueOf(m.getType()))
                        .order(m.getOrder())
                        .build();
                return media;
            }).collect(Collectors.toList()));
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        Integer oldStock = product.getStockQty();

        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQty() != null) product.setStockQty(request.getStockQty());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
            product.setCategory(category);
        }
        product.setActive(request.isActive());

        if (oldStock != null && oldStock == 0 && product.getStockQty() > 0) {
            product.setActive(true);
            log.info("Produit {} réactivé automatiquement (stock: {})", product.getName(), product.getStockQty());
        }

        if (request.getMediaList() != null) {
            product.getMediaList().clear();
            product.getMediaList().addAll(request.getMediaList().stream().map(m -> {
                ProductMedia media = ProductMedia.builder()
                        .product(product)
                        .url(m.getUrl())
                        .type(ProductMedia.MediaType.valueOf(m.getType()))
                        .order(m.getOrder())
                        .build();
                return media;
            }).collect(Collectors.toList()));
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product addMedia(UUID productId, String url, String type) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));
        ProductMedia media = ProductMedia.builder()
                .product(product)
                .url(url)
                .type(ProductMedia.MediaType.valueOf(type))
                .order(product.getMediaList().size() + 1)
                .build();
        product.getMediaList().add(media);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        if (orderRepository.hasOrderItemsForProduct(id)) {
            cartItemRepository.deleteByProductId(id);
            productMediaRepository.deleteByProductId(id);
        }

        product.setActive(false);
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Produit {} désactivé (soft delete)", product.getName());
    }

    @Transactional
    public void restoreProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        product.setActive(true);
        product.setDeleted(false);
        productRepository.save(product);
        log.info("Produit {} restauré", product.getName());
    }

    public DashboardStatsResponse getDashboardStats() {
        long ruptureCount = productRepository.countByStockQty(0);
        long stockFaibleCount = productRepository.countStockFaible(LOW_STOCK_THRESHOLD);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(start, end);
        long commandesDuJour = todayOrders.size();
        BigDecimal caDuJour = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Product> lowStock = productRepository.findTop5ByOrderByStockQtyAsc(PageRequest.of(0, 5));
        List<DashboardStatsResponse.ProduitCritique> critiques = lowStock.stream()
                .map(p -> DashboardStatsResponse.ProduitCritique.builder()
                        .id(p.getId().toString())
                        .nom(p.getName())
                        .stock(p.getStockQty() != null ? p.getStockQty() : 0)
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .produitsEnRupture(ruptureCount)
                .produitsStockFaible(stockFaibleCount)
                .commandesDuJour(commandesDuJour)
                .caDuJour(caDuJour)
                .produitsCritiques(critiques)
                .build();
    }
}
