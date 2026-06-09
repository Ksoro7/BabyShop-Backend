package OCI.BabyShop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQty;
    private String stockStatus;
    private CategoryInfo category;
    private List<MediaInfo> mediaList;
    private String imageUrl;
    private boolean isActive;
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    @Builder
    public static class CategoryInfo {
        private UUID id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class MediaInfo {
        private UUID id;
        private String url;
        private String type;
        private int order;
    }
}
