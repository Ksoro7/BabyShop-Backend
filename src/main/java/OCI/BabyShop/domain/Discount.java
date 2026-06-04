package OCI.BabyShop.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valeur;

    @Column(name = "montant_min_achat", precision = 10, scale = 2)
    private BigDecimal montantMinAchat;

    private LocalDate dateDebut;

    private LocalDate dateExpiration;

    @Column(name = "nb_max_utilisations")
    private Integer nbMaxUtilisations;

    @Column(name = "nb_utilisations", nullable = false)
    @Builder.Default
    private int nbUtilisations = 0;

    @Column(name = "usage_unique_par_user")
    private Boolean usageUniqueParUser;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DiscountType {
        percentage, fixed
    }
}
