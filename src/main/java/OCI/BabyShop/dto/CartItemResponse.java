package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private int quantity;
    private LocalDateTime addedAt;
}
