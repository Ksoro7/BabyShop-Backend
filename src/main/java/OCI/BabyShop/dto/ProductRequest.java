package OCI.BabyShop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductRequest {

    @NotBlank(message = "Le SKU est obligatoire")
    @Size(max = 50, message = "Le SKU ne peut pas dépasser 50 caractères")
    private String sku;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String name;

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être strictement positif")
    private BigDecimal price;

    @NotNull(message = "La quantité en stock est obligatoire")
    @Positive(message = "La quantité en stock doit être positive")
    private Integer stockQty;

    @NotNull(message = "La catégorie est obligatoire")
    private UUID categoryId;

    @JsonProperty("isActive")
    private boolean isActive;

    @Valid
    private List<MediaItem> mediaList;

    @Data
    public static class MediaItem {

        @NotBlank(message = "L'URL du média est obligatoire")
        private String url;

        @NotBlank(message = "Le type du média est obligatoire")
        private String type;

        private Integer order;
    }
}
