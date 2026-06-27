package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserOrderResponse {
    private UUID id;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountApplied;
    private String paymentMethod;
    private String paymentReference;
    private String deliveryDate;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class OrderItemDto {
        private String productName;
        private String productImage;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
