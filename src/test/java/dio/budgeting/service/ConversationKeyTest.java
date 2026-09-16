package dio.budgeting.service;

import dio.budgeting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationKeyTest {

    static final UUID USER = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void should_prefixWithUserId_when_clientSendsConversationId() {
        assertThat(ConversationKey.of(USER, "principal")).isEqualTo(USER + ":principal");
    }

    @Test
    void should_useDefault_when_conversationIdIsNullOrBlank() {
        assertThat(ConversationKey.of(USER, null)).isEqualTo(USER + ":default");
        assertThat(ConversationKey.of(USER, "  ")).isEqualTo(USER + ":default");
    }

    @Test
    void should_reject_when_conversationIdHasInvalidCharacters() {
        assertThatThrownBy(() -> ConversationKey.of(USER, "../outro:usuario"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_reject_when_conversationIdIsTooLong() {
        assertThatThrownBy(() -> ConversationKey.of(USER, "a".repeat(41)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_neverCollide_when_twoUsersUseTheSameId() {
        var other = UUID.randomUUID();
        assertThat(ConversationKey.of(USER, "x")).isNotEqualTo(ConversationKey.of(other, "x"));
    }
}
