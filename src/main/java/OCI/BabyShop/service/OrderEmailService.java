package OCI.BabyShop.service;

import OCI.BabyShop.domain.Order;
import OCI.BabyShop.domain.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

// @change [PROD-READY] Désactivé (service inactif, réactiver via @Service quand mail prêt) - 2026-06-12
// @Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Async
    public void sendOrderConfirmation(Order order) {
        String orderRef = order.getId().toString().substring(0, 8).toUpperCase();
        String customerHtml = buildCustomerHtml(order, orderRef);
        String adminHtml = buildAdminHtml(order, orderRef);
        String subject = "\uD83D\uDED2 Confirmation de votre commande #" + orderRef;

        send(customerHtml, order.getUser().getEmail(), subject);
        send(adminHtml, fromEmail, "Nouvelle commande #" + orderRef);
    }

    @Async
    public void sendOrderConfirmed(Order order) {
        String orderRef = order.getId().toString().substring(0, 8).toUpperCase();
        String html = buildConfirmedHtml(order, orderRef);
        String subject = "\u2705 Votre commande #" + orderRef + " est confirm\u00e9e !";
        send(html, order.getUser().getEmail(), subject);
    }

    private void send(String html, String to, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email envoye a {}", to);
        } catch (Exception e) {
            log.warn("SMTP indisponible ({}), contenu email logge (destinataire: {}) :\n{}", e.getClass().getSimpleName(), to, html);
        }
    }

    private String buildCustomerHtml(Order order, String orderRef) {
        String date = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";
        String discountRow = "";
        if (order.getDiscountApplied() != null && order.getDiscountApplied().compareTo(java.math.BigDecimal.ZERO) > 0) {
            discountRow = """
                    <tr>
                        <td style="padding: 8px 12px; border-bottom: 1px solid #eee; color: #e91e63;">Remise</td>
                        <td style="padding: 8px 12px; border-bottom: 1px solid #eee; text-align: right; color: #e91e63;">- %s FCFA</td>
                    </tr>
                    """.formatted(formatPrice(order.getDiscountApplied()));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;">
                    <table style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;margin-top:20px;margin-bottom:20px;">
                        <tr>
                            <td style="background:linear-gradient(135deg,#e91e63,#ad1457);padding:30px;text-align:center;">
                                <h1 style="color:#fff;margin:0;font-size:24px;">RaBiShop</h1>
                                <p style="color:rgba(255,255,255,.85);margin:5px 0 0;font-size:14px;">Confirmation de commande</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:30px;">
                                <p style="font-size:16px;margin:0 0 5px;">Bonjour <strong>%s</strong>,</p>
                                <p style="color:#666;margin:0 0 20px;">Votre commande a bien ete recue.</p>

                                <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Reference</td>
                                        <td style="text-align:right;font-weight:bold;">#%s</td>
                                    </tr>
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Date</td>
                                        <td style="text-align:right;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Paiement</td>
                                        <td style="text-align:right;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Statut</td>
                                        <td style="text-align:right;">%s</td>
                                    </tr>
                                </table>

                                <h3 style="color:#333;font-size:15px;margin:0 0 10px;">Detail de votre commande</h3>
                                <table style="width:100%%;border-collapse:collapse;">
                                    <thead>
                                        <tr style="background:#f9f9f9;">
                                            <th style="padding:10px 12px;text-align:left;font-size:13px;color:#666;">Produit</th>
                                            <th style="padding:10px 12px;text-align:center;font-size:13px;color:#666;">Qté</th>
                                            <th style="padding:10px 12px;text-align:right;font-size:13px;color:#666;">Prix</th>
                                            <th style="padding:10px 12px;text-align:right;font-size:13px;color:#666;">Sous-total</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        %s
                                        %s
                                        <tr style="font-weight:bold;font-size:16px;">
                                            <td style="padding:12px;border-top:2px solid #e91e63;" colspan="3">Total</td>
                                            <td style="padding:12px;border-top:2px solid #e91e63;text-align:right;">%s FCFA</td>
                                        </tr>
                                    </tbody>
                                </table>

                                <p style="color:#666;font-size:13px;margin-top:25px;">
                                    Nous vous contacterons sous peu pour confirmer.
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="background:#f9f9f9;padding:20px;text-align:center;font-size:12px;color:#999;">
                                L'equipe RaBiShop &mdash; <a href="%s" style="color:#e91e63;">%s</a>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(order.getUser().getFirstName()),
                orderRef,
                escapeHtml(date),
                formatPaymentMethod(order.getPaymentMethod()),
                formatStatus(order.getStatus().name()),
                buildItemsRows(order),
                discountRow,
                formatPrice(order.getTotalAmount()),
                baseUrl, baseUrl
        );
    }

    private String buildAdminHtml(Order order, String orderRef) {
        String date = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;">
                    <table style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;margin-top:20px;margin-bottom:20px;">
                        <tr>
                            <td style="background:#333;padding:30px;text-align:center;">
                                <h1 style="color:#fff;margin:0;font-size:22px;">Nouvelle commande</h1>
                                <p style="color:#aaa;margin:5px 0 0;font-size:14px;">#%s</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:30px;">
                                <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                                    <tr><td style="color:#888;padding:4px 0;">Client</td><td style="text-align:right;">%s %s</td></tr>
                                    <tr><td style="color:#888;padding:4px 0;">Email</td><td style="text-align:right;">%s</td></tr>
                                    <tr><td style="color:#888;padding:4px 0;">Telephone</td><td style="text-align:right;">%s</td></tr>
                                    <tr><td style="color:#888;padding:4px 0;">Paiement</td><td style="text-align:right;">%s</td></tr>
                                    <tr><td style="color:#888;padding:4px 0;">Date</td><td style="text-align:right;">%s</td></tr>
                                </table>

                                <table style="width:100%%;border-collapse:collapse;">
                                    <thead>
                                        <tr style="background:#f9f9f9;">
                                            <th style="padding:10px;text-align:left;font-size:13px;color:#666;">Produit</th>
                                            <th style="padding:10px;text-align:center;font-size:13px;color:#666;">Qté</th>
                                            <th style="padding:10px;text-align:right;font-size:13px;color:#666;">Total</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        %s
                                    </tbody>
                                </table>

                                <p style="margin-top:20px;font-size:16px;font-weight:bold;">Total : %s FCFA</p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                orderRef,
                escapeHtml(order.getUser().getFirstName()),
                escapeHtml(order.getUser().getLastName()),
                escapeHtml(order.getUser().getEmail()),
                escapeHtml(order.getUser().getPhone() != null ? order.getUser().getPhone() : ""),
                formatPaymentMethod(order.getPaymentMethod()),
                escapeHtml(date),
                buildAdminItemsRows(order),
                formatPrice(order.getTotalAmount())
        );
    }

    private String buildConfirmedHtml(Order order, String orderRef) {
        String date = order.getUpdatedAt() != null
                ? order.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;">
                    <table style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;margin-top:20px;margin-bottom:20px;">
                        <tr>
                            <td style="background:linear-gradient(135deg,#4caf50,#2e7d32);padding:30px;text-align:center;">
                                <h1 style="color:#fff;margin:0;font-size:24px;">RaBiShop</h1>
                                <p style="color:rgba(255,255,255,.85);margin:5px 0 0;font-size:14px;">Commande confirmee</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:30px;">
                                <p style="font-size:16px;margin:0 0 5px;">Bonjour <strong>%s</strong>,</p>
                                <p style="color:#666;margin:0 0 20px;">Votre commande <strong>#%s</strong> a ete confirmee.</p>

                                <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Reference</td>
                                        <td style="text-align:right;font-weight:bold;">#%s</td>
                                    </tr>
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Confirmee le</td>
                                        <td style="text-align:right;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="color:#888;font-size:13px;padding:4px 0;">Total</td>
                                        <td style="text-align:right;font-weight:bold;">%s FCFA</td>
                                    </tr>
                                </table>

                                <p style="color:#666;font-size:14px;margin-top:20px;">
                                    Elle sera livree prochainement.
                                </p>

                                <p style="color:#666;font-size:14px;margin-top:15px;">
                                    Merci pour votre confiance !
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="background:#f9f9f9;padding:20px;text-align:center;font-size:12px;color:#999;">
                                L'equipe RaBiShop &mdash; <a href="%s" style="color:#4caf50;">%s</a>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(order.getUser().getFirstName()),
                orderRef,
                orderRef,
                escapeHtml(date),
                formatPrice(order.getTotalAmount()),
                baseUrl, baseUrl
        );
    }

    private String buildItemsRows(Order order) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            sb.append("""
                    <tr>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:center;">%d</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:right;">%s FCFA</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:right;">%s FCFA</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProduct().getName()),
                    item.getQuantity(),
                    formatPrice(item.getUnitPrice()),
                    formatPrice(item.getSubtotal())
            ));
        }
        return sb.toString();
    }

    private String buildAdminItemsRows(Order order) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            sb.append("""
                    <tr>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:center;">%d</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:right;">%s FCFA</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProduct().getName()),
                    item.getQuantity(),
                    formatPrice(item.getSubtotal())
            ));
        }
        return sb.toString();
    }

    private String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "0";
        long whole = price.longValue();
        return String.format("%,d", whole);
    }

    // @change [PROD-READY] Suppression du cas CINETPAY dans le formattage - 2026-06-12
    private String formatPaymentMethod(String method) {
        if (method == null) return "WhatsApp";
        return switch (method.toUpperCase()) {
            case "WHATSAPP" -> "Commande WhatsApp";
            default -> method;
        };
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "En attente";
            case "PAID" -> "Payee";
            case "IN_PROGRESS" -> "En cours";
            case "SHIPPED" -> "Expediee";
            case "DELIVERED" -> "Livree";
            case "CANCELLED" -> "Annulee";
            default -> status;
        };
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
