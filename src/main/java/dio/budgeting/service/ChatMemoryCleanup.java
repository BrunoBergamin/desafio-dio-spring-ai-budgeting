package dio.budgeting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Apaga conversas antigas da memoria da Lumi.
 * <p>
 * Com a memoria em RAM o limite era o numero de conversas guardadas, porque tudo disputava o heap.
 * No banco o problema muda: espaco em disco e barato, mas guardar conversa de um ano atras nao ajuda
 * ninguem e ainda e dado pessoal parado. Entao o criterio virou tempo: o que passou da janela sai.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.chat", name = "retention-days")
public class ChatMemoryCleanup {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final int retentionDays;

    public ChatMemoryCleanup(JdbcTemplate jdbcTemplate, Clock clock,
                             @Value("${app.chat.retention-days:30}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /** Uma vez por dia, de madrugada, junto com o resto da manutencao. */
    @Scheduled(cron = "0 15 0 * * *", zone = "${app.timezone:America/Sao_Paulo}")
    public int purgeOldConversations() {
        var limit = LocalDateTime.now(clock).minusDays(retentionDays);
        var removed = jdbcTemplate.update("DELETE FROM SPRING_AI_CHAT_MEMORY WHERE timestamp < ?", limit);
        if (removed > 0) {
            log.info("[memoria] {} mensagens com mais de {} dias apagadas", removed, retentionDays);
        }
        return removed;
    }
}
