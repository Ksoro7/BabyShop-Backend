package OCI.BabyShop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderEmailItem {
    @NotBlank
    private String productName;

    @Min(1)
    private int quantity;

    @NotNull
    private BigDecimal unitPrice;
}
