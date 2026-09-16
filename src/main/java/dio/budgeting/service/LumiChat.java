package dio.budgeting.service;

import dio.budgeting.tool.ToolUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Toda a conversa com o Spring AI passa por aqui: system prompt com a data de hoje,
 * usuario no {@code ToolContext} (para as ferramentas) e chave da memoria de conversa.
 */
@Slf4j
@Component
public class LumiChat {

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)", Locale.forLanguageTag("pt-BR"));

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final Resource systemPrompt;

    public LumiChat(ChatClient chatClient,
                    ChatMemory chatMemory,
                    @Value("classpath:prompts/system-message.st") Resource systemPrompt) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.systemPrompt = systemPrompt;
    }

    public String answer(java.util.UUID userId, String conversationKey, String message) {
        var answer = chatClient.prompt()
                .system(system -> system.text(systemPrompt).param("today", LocalDate.now().format(TODAY_FORMAT)))
                .toolContext(Map.of(ToolUser.USER_ID, userId))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationKey))
                .user(message)
                .call()
                .content();
        log.info("[lumi] user={} conversa={} pergunta='{}' | resposta='{}'", userId, conversationKey, message, answer);
        return answer;
    }

    public void forget(String conversationKey) {
        chatMemory.clear(conversationKey);
        log.info("[lumi] conversa apagada: {}", conversationKey);
    }
}
