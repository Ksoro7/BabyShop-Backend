package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromoValidationResponse {
    private boolean valide;
    private BigDecimal remise;
    private String type;
    private String message;
}
