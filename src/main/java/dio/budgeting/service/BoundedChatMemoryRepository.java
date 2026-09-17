package dio.budgeting.service;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator sobre um {@link ChatMemoryRepository} que limita a quantidade de conversas guardadas.
 * <p>
 * A janela do {@code MessageWindowChatMemory} limita as mensagens DE CADA conversa, mas nao o numero de conversas:
 * como o id da conversa vem do cliente ({@code /api/assistant/chat?conversationId=...}), sem este limite um
 * cliente conseguiria criar conversas sem parar e a memoria da JVM cresceria junto. Aqui as conversas ficam em
 * ordem de uso (LRU): ao passar do limite, a conversa parada ha mais tempo e apagada do repositorio de verdade.
 * <p>
 * O repositorio decorado continua sendo a unica fonte das mensagens; esta classe so decide quem fica.
 */
public class BoundedChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final Map<String, Boolean> recentlyUsed;

    public BoundedChatMemoryRepository(ChatMemoryRepository delegate, int maxConversations) {
        if (maxConversations < 1) {
            throw new IllegalArgumentException("maxConversations precisa ser pelo menos 1");
        }
        this.delegate = delegate;
        this.recentlyUsed = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                if (size() <= maxConversations) {
                    return false;
                }
                delegate.deleteByConversationId(eldest.getKey());
                return true;
            }
        };
    }

    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds();
    }

    @Override
    public synchronized List<Message> findByConversationId(String conversationId) {
        recentlyUsed.get(conversationId);
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public synchronized void saveAll(String conversationId, List<Message> messages) {
        delegate.saveAll(conversationId, messages);
        recentlyUsed.put(conversationId, Boolean.TRUE);
    }

    @Override
    public synchronized void deleteByConversationId(String conversationId) {
        recentlyUsed.remove(conversationId);
        delegate.deleteByConversationId(conversationId);
    }
}
