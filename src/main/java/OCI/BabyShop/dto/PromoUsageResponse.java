package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PromoUsageResponse {
    private UUID id;
    private String userEmail;
    private UUID discountId;
    private String discountCode;
    private UUID orderId;
    private BigDecimal montantRemise;
    private LocalDateTime usedAt;
}
