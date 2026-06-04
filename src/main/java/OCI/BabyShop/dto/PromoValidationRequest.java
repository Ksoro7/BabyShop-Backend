package OCI.BabyShop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PromoValidationRequest {
    @NotBlank
    private String code;

    @PositiveOrZero
    private BigDecimal montantPanier;

    private UUID userId;
}
