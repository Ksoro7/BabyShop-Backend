package OCI.BabyShop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cinetpay")
public class CinetPayConfig {
    private String apiKey;
    private String siteId;
    private String notifyUrl;
    private String returnUrl;
    private boolean sandbox = false;
}
