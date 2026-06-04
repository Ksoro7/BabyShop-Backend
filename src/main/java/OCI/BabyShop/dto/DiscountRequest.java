package OCI.BabyShop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DiscountRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String type;

    @Positive
    private BigDecimal valeur;

    @PositiveOrZero
    private BigDecimal montantMinAchat;

    private LocalDate dateDebut;

    private LocalDate dateExpiration;

    private Integer nbMaxUtilisations;

    private Boolean usageUniqueParUser;

    private boolean actif = true;
}
