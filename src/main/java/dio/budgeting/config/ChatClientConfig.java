package dio.budgeting.config;

import dio.budgeting.service.BoundedChatMemoryRepository;
import dio.budgeting.tool.BudgetTools;
import dio.budgeting.tool.RecurringTools;
import dio.budgeting.tool.TransactionTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * Memoria de conversa em RAM, com janela de poucas mensagens: barata, deterministica e suficiente
     * para uma conversa curta. Some ao reiniciar e nao funciona com varias instancias: escolha consciente,
     * documentada no README. O {@link BoundedChatMemoryRepository} limita tambem o NUMERO de conversas
     * (o id vem do cliente), para a memoria da JVM nao crescer sem controle. O advisor de memoria roda ANTES do de tool calling, entao o historico nao
     * guarda as idas e vindas das ferramentas, so o par pergunta/resposta.
     */
    @Bean
    ChatMemory chatMemory(@Value("${app.chat.memory-window:10}") int window,
                          @Value("${app.chat.max-conversations:200}") int maxConversations) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new BoundedChatMemoryRepository(new InMemoryChatMemoryRepository(), maxConversations))
                .maxMessages(window)
                .build();
    }

    /** O ChatClient fica configurado em um único lugar, com as ferramentas e a memória registradas. */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, TransactionTools transactionTools,
                          BudgetTools budgetTools, RecurringTools recurringTools, ChatMemory chatMemory) {
        return builder
                .defaultTools(transactionTools, budgetTools, recurringTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
