package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MediaResponse {
    private UUID id;
    private String url;
    private String type;
    private String productName;
    private UUID productId;
    private LocalDateTime uploadedAt;
}
