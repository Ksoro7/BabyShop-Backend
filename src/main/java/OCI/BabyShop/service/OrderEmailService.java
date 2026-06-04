package OCI.BabyShop.service;

import OCI.BabyShop.dto.OrderEmailItem;
import OCI.BabyShop.dto.OrderEmailRequest;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOrderEmail(OrderEmailRequest request) {
        String html = buildHtml(request);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo("babyshop@gmail.com");
            helper.setSubject("Nouvelle commande reçue !");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email de commande envoyé à babyshop@gmail.com");
        } catch (MailSendException | MessagingException e) {
            log.warn("SMTP indisponible, contenu de l'email loggé ci-dessous :");
            log.warn("À : babyshop@gmail.com");
            log.warn("Sujet : Nouvelle commande reçue !");
            log.warn("Corps HTML :\n{}", html);
        }
    }

    private String buildHtml(OrderEmailRequest request) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderEmailItem item : request.getItems()) {
            itemsHtml.append("<li>")
                    .append(escapeHtml(item.getProductName()))
                    .append(" x").append(item.getQuantity())
                    .append(" &mdash; ").append(item.getUnitPrice())
                    .append(" FCFA</li>");
        }

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #e91e63;">Nouvelle commande reçue !</h2>
                    <p><strong>Client :</strong> %s</p>
                    <p><strong>Email :</strong> %s</p>
                    <h3>Produits :</h3>
                    <ul>%s</ul>
                    <p><strong>Total :</strong> %.2f FCFA</p>
                    <p><strong>Date :</strong> %s</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(request.getClientName()),
                escapeHtml(request.getClientEmail()),
                itemsHtml,
                request.getTotal(),
                date
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
