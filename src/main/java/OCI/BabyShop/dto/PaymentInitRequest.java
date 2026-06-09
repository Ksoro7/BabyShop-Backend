package OCI.BabyShop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentInitRequest {
    @NotNull
    private UUID orderId;
}
