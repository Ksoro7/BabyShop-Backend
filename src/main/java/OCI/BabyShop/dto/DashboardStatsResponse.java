package OCI.BabyShop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long produitsEnRupture;
    private long produitsStockFaible;
    private long commandesDuJour;
    private BigDecimal caDuJour;
    private List<ProduitCritique> produitsCritiques;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProduitCritique {
        private String id;
        private String nom;
        private int stock;
    }
}
