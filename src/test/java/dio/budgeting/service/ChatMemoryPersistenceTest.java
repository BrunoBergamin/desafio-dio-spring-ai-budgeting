package dio.budgeting.service;

import dio.budgeting.support.JpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A memoria da Lumi agora vive no banco. Aqui o que importa e a tabela: ela aceita a chave
 * "uuid:conversa" (que passa dos 36 caracteres do schema padrao do Spring AI) e a limpeza tira
 * o que e velho sem levar junto o que e recente.
 */
@JpaTest
class ChatMemoryPersistenceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"), SP);

    @Autowired JdbcTemplate jdbcTemplate;

    private void insert(String conversationId, String content, LocalDateTime timestamp, long sequence) {
        jdbcTemplate.update("""
                INSERT INTO SPRING_AI_CHAT_MEMORY (conversation_id, content, type, timestamp, sequence_id)
                VALUES (?, ?, 'USER', ?, ?)
                """, conversationId, content, timestamp, sequence);
    }

    @Test
    void should_acceptTheConversationKey_when_itIsLongerThanTheDefaultSchemaAllows() {
        // "uuid:nome-da-conversa" tem mais de 36 caracteres; com o tamanho padrao o insert falharia
        var key = "11111111-1111-1111-1111-111111111111:conversa-do-whatsapp";
        assertThat(key.length()).isGreaterThan(36);

        insert(key, "gastei 80 reais no mercado", LocalDateTime.now(TODAY), 1);

        var saved = jdbcTemplate.queryForObject(
                "SELECT content FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?", String.class, key);
        assertThat(saved).isEqualTo("gastei 80 reais no mercado");
    }

    @Test
    void should_removeOnlyWhatIsOlderThanTheWindow_when_cleaningUp() {
        var recente = LocalDateTime.now(TODAY).minusDays(3);
        var antiga = LocalDateTime.now(TODAY).minusDays(31);
        insert("user:recente", "conversa de ontem", recente, 1);
        insert("user:antiga", "conversa do mês passado", antiga, 1);

        var removed = new ChatMemoryCleanup(jdbcTemplate, TODAY, 30).purgeOldConversations();

        assertThat(removed).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_AI_CHAT_MEMORY", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT conversation_id FROM SPRING_AI_CHAT_MEMORY", String.class)).isEqualTo("user:recente");
    }

    @Test
    void should_removeNothing_when_everyConversationIsRecent() {
        insert("user:a", "oi", LocalDateTime.now(TODAY).minusDays(1), 1);

        assertThat(new ChatMemoryCleanup(jdbcTemplate, TODAY, 30).purgeOldConversations()).isZero();
    }
}
