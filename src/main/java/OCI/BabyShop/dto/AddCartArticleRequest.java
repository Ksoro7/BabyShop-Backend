package OCI.BabyShop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddCartArticleRequest {
    @NotNull
    private UUID productId;

    @NotBlank
    private String productName;

    private String imageUrl;

    @NotNull
    private BigDecimal price;

    @Min(1)
    private int quantity;
}
