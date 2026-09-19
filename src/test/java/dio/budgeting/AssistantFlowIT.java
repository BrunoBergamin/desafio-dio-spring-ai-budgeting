package dio.budgeting;

import dio.budgeting.entity.Category;
import dio.budgeting.entity.User;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import dio.budgeting.service.AssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de ponta a ponta que chamam a OpenAI de verdade (custam alguns centavos).
 * Só rodam quando a variável OPENAI_API_KEY está definida.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.+")
class AssistantFlowIT {

    @Autowired AssistantService assistantService;
    @Autowired TransactionRepository transactionRepository;
    @Autowired UserRepository userRepository;

    UUID bruno;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();
        bruno = userRepository.save(new User("Bruno", "bruno@it.com", "hash")).getId();
        assistantService.forget(bruno, null);
    }

    @Test
    void should_persistTransaction_when_textCommandIsSent() {
        var response = assistantService.chat(bruno, null, "Gastei 42 reais e 90 centavos na farmácia comprando remédio");

        var saved = transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(bruno);
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getAmount()).isEqualByComparingTo("42.90");
        assertThat(saved.getFirst().getCategory()).isEqualTo(Category.PHARMA);
        System.out.println(response.answer());
    }

    @Test
    void should_notPersist_when_amountIsMissing() {
        var response = assistantService.chat(bruno, null, "Fui ao mercado hoje");

        assertThat(transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(bruno)).isEmpty();
        System.out.println(response.answer());
    }

    @Test
    void should_runFullVoiceFlow_when_audioIsSent() throws IOException {
        var audio = new ClassPathResource("audio/recording-1.m4a");
        var file = new MockMultipartFile("file", "recording-1.m4a", "audio/m4a", audio.getInputStream());

        var mp3 = assistantService.voiceToVoice(bruno, null, file);

        assertThat(transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(bruno)).hasSize(1);
        assertThat(mp3).hasSizeGreaterThan(1024);
    }
}
