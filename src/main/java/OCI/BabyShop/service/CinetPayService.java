package OCI.BabyShop.service;

import OCI.BabyShop.config.CinetPayConfig;
import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.OrderStatus;
import OCI.BabyShop.dto.PaymentInitResponse;
import OCI.BabyShop.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinetPayService {

    private final CinetPayConfig config;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String PROD_API_URL = "https://api.cinetpay.com/v2";
    private static final String SANDBOX_API_URL = "https://sandbox-api.cinetpay.com/v2";

    public String getApiBaseUrl() {
        return config.isSandbox() ? SANDBOX_API_URL : PROD_API_URL;
    }

    @Transactional
    public PaymentInitResponse initiatePayment(UUID orderId, String returnUrl) {
        if (config.getApiKey() == null || config.getApiKey().isBlank() ||
                config.getSiteId() == null || config.getSiteId().isBlank()) {
            log.warn("CinetPay non configuré : clés API manquantes");
            return PaymentInitResponse.builder()
                    .success(false)
                    .message("CinetPay n'est pas configuré. Veuillez renseigner CINETPAY_API_KEY et CINETPAY_SITE_ID.")
                    .build();
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));

        String transactionId = "RBS-" + orderId.toString().substring(0, 8).toUpperCase()
                + "-" + System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("apikey", config.getApiKey());
        body.put("site_id", config.getSiteId());
        body.put("transaction_id", transactionId);
        body.put("amount", order.getTotalAmount().intValue());
        body.put("currency", "XOF");
        body.put("description", "Commande RaBiShop #" + orderId.toString().substring(0, 8).toUpperCase());
        body.put("customer_name", order.getUser().getFirstName() + " " + order.getUser().getLastName());
        body.put("customer_email", order.getUser().getEmail());
        String phone = order.getUser().getPhone() != null
                ? order.getUser().getPhone().replaceAll("[^0-9]", "") : "";
        body.put("customer_phone", phone);
        body.put("notify_url", config.getNotifyUrl());
        body.put("return_url", returnUrl != null && !returnUrl.isBlank() ? returnUrl : config.getReturnUrl());
        body.put("channels", "ALL");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = getApiBaseUrl() + "/payment";
            log.info("Initiation paiement CinetPay : {}", url);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode responseBody = response.getBody();
            if (responseBody == null) {
                return PaymentInitResponse.builder()
                        .success(false).message("Réponse vide de CinetPay").build();
            }

            String code = responseBody.get("code").asText();
            String msg = responseBody.get("message").asText();

            if ("00".equals(code)) {
                JsonNode data = responseBody.get("data");
                String paymentUrl = data.get("payment_url").asText();
                String paymentToken = data.get("payment_token").asText();

                order.setPaymentReference(transactionId);
                order.setPaymentMethod("CINETPAY");
                orderRepository.save(order);

                log.info("Paiement initié : transactionId={}", transactionId);

                return PaymentInitResponse.builder()
                        .success(true)
                        .message("Paiement initié avec succès")
                        .paymentUrl(paymentUrl)
                        .paymentToken(paymentToken)
                        .transactionId(transactionId)
                        .build();
            } else {
                log.warn("Échec CinetPay : code={}, message={}", code, msg);
                return PaymentInitResponse.builder()
                        .success(false).message("Erreur CinetPay : " + msg).build();
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation du paiement CinetPay", e);
            return PaymentInitResponse.builder()
                    .success(false)
                    .message("Erreur de communication avec CinetPay : " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        log.info("Webhook CinetPay reçu : {}", payload);
        String transactionId = (String) payload.get("transaction_id");
        String status = (String) payload.get("status");

        if (transactionId == null) {
            log.warn("transaction_id manquant dans le webhook");
            return;
        }

        Optional<Order> optOrder = orderRepository.findByPaymentReference(transactionId);
        if (optOrder.isEmpty()) {
            log.warn("Aucune commande trouvée pour la transaction : {}", transactionId);
            return;
        }

        Order order = optOrder.get();

        if ("ACCEPTED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Commande {} marquée PAYÉE", order.getId());
        } else if ("REFUSED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            log.warn("Paiement refusé pour la commande {}", order.getId());
        }
    }
}
