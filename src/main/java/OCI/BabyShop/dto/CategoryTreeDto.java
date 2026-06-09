package OCI.BabyShop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class CategoryTreeDto {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private List<CategoryTreeDto> children;
}
