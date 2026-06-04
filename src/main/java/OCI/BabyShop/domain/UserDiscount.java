package OCI.BabyShop.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_discounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDiscount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String discountCode;

    @Column(nullable = false)
    private BigDecimal percentage;

    private LocalDateTime validUntil;
    
    private boolean isUsed;
}
