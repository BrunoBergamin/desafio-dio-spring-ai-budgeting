package dio.budgeting.config;

import dio.budgeting.service.ChatMemoryCleanup;
import dio.budgeting.tool.BudgetTools;
import dio.budgeting.tool.GoalTools;
import dio.budgeting.tool.RecurringTools;
import dio.budgeting.tool.ReportTools;
import dio.budgeting.tool.TransactionTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * Memoria de conversa no banco: o {@code JdbcChatMemoryRepository} vem do starter, montado sobre o
     * mesmo DataSource da aplicacao. A Lumi lembra do contexto mesmo depois de reiniciar, e a tabela e
     * criada pelo Flyway (V8), nao pelo starter, porque a chave daqui e maior do que o padrao dele.
     * <p>
     * A janela continua curta: so as ultimas mensagens entram no prompt, entao a conta com o modelo nao
     * cresce com o tempo. Quem tira o historico velho do banco e o {@link ChatMemoryCleanup}.
     * O advisor de memoria roda ANTES do de tool calling, entao o historico guarda so o par
     * pergunta/resposta, e nao as idas e vindas das ferramentas.
     */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
                          @Value("${app.chat.memory-window:10}") int window) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(window)
                .build();
    }

    /** O ChatClient fica configurado em um único lugar, com as ferramentas e a memória registradas. */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, TransactionTools transactionTools,
                          BudgetTools budgetTools, RecurringTools recurringTools,
                          GoalTools goalTools, ReportTools reportTools, ChatMemory chatMemory) {
        return builder
                .defaultTools(transactionTools, budgetTools, recurringTools, goalTools, reportTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
