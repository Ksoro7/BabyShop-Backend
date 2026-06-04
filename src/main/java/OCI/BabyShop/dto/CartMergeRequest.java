package OCI.BabyShop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CartMergeRequest {
    @NotNull
    @Valid
    private List<MergeItem> items;

    @Data
    public static class MergeItem {
        @NotNull
        private UUID productId;

        @Min(1)
        private int quantity;
    }
}
