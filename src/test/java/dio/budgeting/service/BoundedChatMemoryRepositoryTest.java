package dio.budgeting.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedChatMemoryRepositoryTest {

    @Test
    void should_dropTheLeastRecentlyUsedConversation_when_limitIsExceeded() {
        var inner = new InMemoryChatMemoryRepository();
        var repository = new BoundedChatMemoryRepository(inner, 2);

        repository.saveAll("a", List.of(new UserMessage("oi a")));
        repository.saveAll("b", List.of(new UserMessage("oi b")));
        repository.findByConversationId("a"); // "a" volta a ser a mais recente
        repository.saveAll("c", List.of(new UserMessage("oi c")));

        assertThat(inner.findConversationIds()).containsExactlyInAnyOrder("a", "c");
        assertThat(repository.findByConversationId("b")).isEmpty();
    }

    @Test
    void should_keepMessagesOfSurvivingConversations() {
        var repository = new BoundedChatMemoryRepository(new InMemoryChatMemoryRepository(), 1);

        repository.saveAll("unica", List.of(new UserMessage("gastei 10")));
        repository.saveAll("unica", List.of(new UserMessage("gastei 10"), new UserMessage("e 20")));

        assertThat(repository.findByConversationId("unica")).hasSize(2);
        assertThat(repository.findConversationIds()).containsExactly("unica");
    }

    @Test
    void should_forgetConversation_when_deleted() {
        var repository = new BoundedChatMemoryRepository(new InMemoryChatMemoryRepository(), 5);
        repository.saveAll("x", List.of(new UserMessage("oi")));

        repository.deleteByConversationId("x");

        assertThat(repository.findConversationIds()).isEmpty();
        assertThat(repository.findByConversationId("x")).isEmpty();
    }

    @Test
    void should_rejectLimitBelowOne() {
        assertThatThrownBy(() -> new BoundedChatMemoryRepository(new InMemoryChatMemoryRepository(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
