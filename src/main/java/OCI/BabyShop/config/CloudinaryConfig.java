package OCI.BabyShop.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    @ConditionalOnExpression(
        "!'${cloudinary.cloud-name:}'.isEmpty()"
    )
    public Cloudinary cloudinary(CloudinaryProperties props) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", props.getCloudName(),
                "api_key", props.getApiKey(),
                "api_secret", props.getApiSecret()
        ));
    }
}
