package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminOrderResponse {
    private UUID id;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String status;
    private String deliveryDate;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class OrderItemDto {
        private UUID id;
        private String productName;
        private String productImage;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
