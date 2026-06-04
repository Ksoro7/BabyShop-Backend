package OCI.BabyShop.repository;

import OCI.BabyShop.domain.OrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, UUID> {
}
