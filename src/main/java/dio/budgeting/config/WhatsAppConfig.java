package dio.budgeting.config;

import dio.budgeting.whatsapp.WhatsAppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Liga as propriedades do WhatsApp e o @Async usado para responder fora da thread do webhook. */
@Configuration
@EnableAsync
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {
}
