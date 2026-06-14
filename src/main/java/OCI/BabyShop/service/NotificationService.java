package OCI.BabyShop.service;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.OrderNotification;
import OCI.BabyShop.repository.OrderNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // @change [PROD-READY] Suppression email (OrderEmailService désactivé) - 2026-06-12
    private final OrderNotificationRepository notificationRepository;

    public void sendOrderNotifications(Order order) {
        try {
            saveNotification(order, OrderNotification.NotificationChannel.WHATSAPP, OrderNotification.NotificationStatus.SENT);
        } catch (Exception e) {
            log.warn("Impossible de sauvegarder la notification WHATSAPP: {}", e.getMessage());
        }
    }

    @Transactional
    private void saveNotification(Order order, OrderNotification.NotificationChannel channel,
                                  OrderNotification.NotificationStatus status) {
        OrderNotification notif = OrderNotification.builder()
                .order(order)
                .channel(channel)
                .status(status)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notif);
    }
}
