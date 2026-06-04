package OCI.BabyShop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderEmailRequest {
    @NotBlank
    private String clientName;

    @NotBlank
    private String clientEmail;

    @NotNull
    @Valid
    private List<OrderEmailItem> items;

    @NotNull
    private BigDecimal total;
}
