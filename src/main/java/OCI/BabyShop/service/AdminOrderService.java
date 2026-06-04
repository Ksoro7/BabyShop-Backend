package OCI.BabyShop.service;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.OrderStatus;
import OCI.BabyShop.dto.AdminOrderResponse;
import OCI.BabyShop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(AdminOrderResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));
        return toResponse(order);
    }

    @Transactional
    public AdminOrderResponse updateStatus(UUID id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));
        order.setStatus(OrderStatus.valueOf(status));
        orderRepository.save(order);
        return toResponse(order);
    }

    private AdminOrderResponse toResponse(Order order) {
        BigDecimal subtotal = order.getItems().stream()
                .map(i -> i.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AdminOrderResponse.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    String imageUrl = item.getProduct().getMediaList().isEmpty() ? null
                            : item.getProduct().getMediaList().get(0).getUrl();
                    return AdminOrderResponse.OrderItemDto.builder()
                            .id(item.getId())
                            .productName(item.getProduct().getName())
                            .productImage(imageUrl)
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build();
                })
                .collect(Collectors.toList());

        String customerName = order.getUser().getFirstName() + " " + order.getUser().getLastName();
        if (customerName.isBlank()) customerName = order.getUser().getEmail();

        return AdminOrderResponse.builder()
                .id(order.getId())
                .customerName(customerName)
                .customerEmail(order.getUser().getEmail())
                .customerPhone(order.getUser().getPhone())
                .subtotal(subtotal)
                .discount(order.getDiscountApplied())
                .total(order.getTotalAmount())
                .status(order.getStatus().name())
                .deliveryDate(order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : null)
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
