package dio.budgeting.config;

import dio.budgeting.tool.TransactionTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * O ChatClient fica configurado em um único lugar, com as ferramentas registradas.
     * O system prompt é aplicado por requisição no AssistantService, pois depende da data de hoje.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, TransactionTools transactionTools) {
        return builder
                .defaultTools(transactionTools)
                .build();
    }
}
