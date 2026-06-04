package OCI.BabyShop.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    public enum NotificationChannel {
        WHATSAPP, EMAIL
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED
    }
}
