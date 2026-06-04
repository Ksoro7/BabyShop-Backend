package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminCartResponse {
    private UUID id;
    private String customerName;
    private String customerEmail;
    private List<CartArticleDto> articles;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CartArticleDto {
        private UUID id;
        private UUID productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
    }
}
