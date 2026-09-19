package dio.budgeting.service;

import dio.budgeting.tool.ToolUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Clock;
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
    private final Clock clock;

    public LumiChat(ChatClient chatClient,
                    ChatMemory chatMemory,
                    @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                    Clock clock) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.systemPrompt = systemPrompt;
        this.clock = clock;
    }

    public String answer(java.util.UUID userId, String conversationKey, String message) {
        var answer = dropRepeatedOpening(chatClient.prompt()
                .system(system -> system.text(systemPrompt).param("today", LocalDate.now(clock).format(TODAY_FORMAT)))
                .toolContext(Map.of(ToolUser.USER_ID, userId))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationKey))
                .user(message)
                .call()
                .content());
        // O conteudo da conversa e dado financeiro pessoal: em INFO fica so o tamanho, o texto vai para DEBUG
        log.info("[lumi] user={} conversa={} pergunta={} chars | resposta={} chars",
                userId, conversationKey, message.length(), answer == null ? 0 : answer.length());
        log.debug("[lumi] pergunta='{}' | resposta='{}'", message, answer);
        return answer;
    }

    /**
     * Alguns modelos (o gpt-oss da Groq, por exemplo) mandam uma frase "de passagem" junto com a chamada da
     * ferramenta e depois a resposta final; o {@code ToolCallingAdvisor} junta os dois textos e a pessoa recebe
     * "Neste mês você gastou X.Neste mês você gastou X. ...". Se a primeira frase aparece de novo mais adiante,
     * fica so a partir da segunda ocorrencia, que e a resposta completa.
     */
    static String dropRepeatedOpening(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        var firstStop = answer.indexOf(". ");
        var opening = firstStop > 0 ? answer.substring(0, firstStop + 1) : null;
        if (opening == null) {
            return answer;
        }
        var again = answer.indexOf(opening, opening.length());
        return again > 0 ? answer.substring(again).strip() : answer;
    }

    public void forget(String conversationKey) {
        chatMemory.clear(conversationKey);
        log.info("[lumi] conversa apagada: {}", conversationKey);
    }
}
