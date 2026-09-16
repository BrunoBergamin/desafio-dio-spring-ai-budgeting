package dio.budgeting.service;

import dio.budgeting.tool.ToolUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.io.ByteArrayResource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LumiChatTest {

    static final UUID USER = UUID.randomUUID();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Mock
    ChatMemory chatMemory;

    @Test
    @SuppressWarnings("unchecked")
    void should_passAuthenticatedUserInToolContext_when_answering() {
        var lumi = new LumiChat(chatClient, chatMemory, new ByteArrayResource("Hoje é {today}.".getBytes()));
        when(chatClient.prompt().system(any(java.util.function.Consumer.class)).toolContext(anyMap())
                .advisors(any(java.util.function.Consumer.class)).user(anyString()).call().content())
                .thenReturn("Registrei.");

        var answer = lumi.answer(USER, USER + ":default", "gastei 10 reais");

        assertThat(answer).isEqualTo("Registrei.");
        // Deep stubs devolvem sempre o mesmo mock intermediario, entao da para verificar nele
        java.util.function.Consumer<ChatClient.PromptSystemSpec> noop = spec -> { };
        var requestSpec = chatClient.prompt().system(noop);
        var captor = ArgumentCaptor.forClass(Map.class);
        // A propria stubbing acima ja conta como uma chamada; a ultima e a real
        verify(requestSpec, org.mockito.Mockito.atLeastOnce()).toolContext(captor.capture());
        assertThat(captor.getValue()).containsEntry(ToolUser.USER_ID, USER);
    }

    @Test
    void should_clearOnlyThatConversation_when_forgetting() {
        var lumi = new LumiChat(chatClient, chatMemory, new ByteArrayResource("x".getBytes()));

        lumi.forget(USER + ":principal");

        verify(chatMemory).clear(USER + ":principal");
    }
}
