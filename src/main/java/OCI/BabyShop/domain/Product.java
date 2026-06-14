package OCI.BabyShop.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "products")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Product {

    public static final int LOW_STOCK_THRESHOLD = 10;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("product")
    private List<ProductMedia> mediaList = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonProperty("isActive")
    private boolean isActive;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    @Version
    private Long version = 0L;

    public enum StockStatus {
        DISPONIBLE, STOCK_FAIBLE, RUPTURE
    }

    public StockStatus getStockStatus() {
        if (stockQty == null || stockQty == 0) return StockStatus.RUPTURE;
        if (stockQty <= LOW_STOCK_THRESHOLD) return StockStatus.STOCK_FAIBLE;
        return StockStatus.DISPONIBLE;
    }
}
