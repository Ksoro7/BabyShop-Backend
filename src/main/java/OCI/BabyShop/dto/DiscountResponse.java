package OCI.BabyShop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DiscountResponse {
    private UUID id;
    private String code;
    private String type;
    private BigDecimal valeur;
    private BigDecimal montantMinAchat;
    private LocalDate dateDebut;
    private LocalDate dateExpiration;
    private Integer nbMaxUtilisations;
    private int nbUtilisations;
    private Boolean usageUniqueParUser;
    private boolean actif;
}
