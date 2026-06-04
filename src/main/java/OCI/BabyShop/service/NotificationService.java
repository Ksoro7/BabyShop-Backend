package OCI.BabyShop.service;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.OrderNotification;
import OCI.BabyShop.repository.OrderNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final OrderNotificationRepository notificationRepository;

    @Async
    public void sendOrderNotifications(Order order) {
        log.info("Simulation de l'envoi de WhatsApp pour la commande: " + order.getId());
        saveNotification(order, OrderNotification.NotificationChannel.WHATSAPP);

        log.info("Simulation de l'envoi de l'e-mail pour la commande: " + order.getId());
        saveNotification(order, OrderNotification.NotificationChannel.EMAIL);
    }

    private void saveNotification(Order order, OrderNotification.NotificationChannel channel) {
        OrderNotification notif = OrderNotification.builder()
                .order(order)
                .channel(channel)
                .status(OrderNotification.NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notif);
    }
}
