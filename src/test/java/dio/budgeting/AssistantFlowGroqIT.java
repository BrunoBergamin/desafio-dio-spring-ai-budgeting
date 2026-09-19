package dio.budgeting;

import dio.budgeting.dto.request.BudgetRequest;
import dio.budgeting.entity.Category;
import dio.budgeting.entity.User;
import dio.budgeting.exception.FeatureUnavailableException;
import dio.budgeting.repository.BudgetRepository;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.repository.UserRepository;
import dio.budgeting.service.AssistantService;
import dio.budgeting.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fluxo de ponta a ponta no perfil gratuito (Groq), chamando a IA de verdade.
 * Só roda quando a variável GROQ_API_KEY está definida.
 */
@SpringBootTest
@ActiveProfiles("groq")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = "gsk_.+")
class AssistantFlowGroqIT {

    @Autowired AssistantService assistantService;
    @Autowired BudgetService budgetService;
    @Autowired TransactionRepository transactionRepository;
    @Autowired BudgetRepository budgetRepository;
    @Autowired UserRepository userRepository;

    UUID bruno;
    UUID outra;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        userRepository.deleteAll();
        bruno = userRepository.save(new User("Bruno", "bruno@it.com", "hash")).getId();
        outra = userRepository.save(new User("Outra", "outra@it.com", "hash")).getId();
        assistantService.forget(bruno, null);
        assistantService.forget(outra, null);
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
    void should_transcribeAndAnswer_when_audioIsSent() throws IOException {
        var audio = new ClassPathResource("audio/recording-1.m4a");
        var file = new MockMultipartFile("file", "recording-1.m4a", "audio/m4a", audio.getInputStream());

        var response = assistantService.voiceToText(bruno, null, file);

        assertThat(response.transcription()).containsIgnoringCase("reais");
        assertThat(transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(bruno)).hasSize(1);
        System.out.println(response.transcription() + " -> " + response.answer());
    }

    @Test
    void should_isolateUsers_when_askingForTotals() {
        assistantService.chat(bruno, null, "Gastei 80 reais no mercado hoje");

        var answer = assistantService.chat(outra, null, "Quanto eu gastei este mês? Responda só o número.");

        assertThat(transactionRepository.findTop5ByUserIdOrderByDateDescCreatedAtDesc(outra)).isEmpty();
        assertThat(answer.answer()).doesNotContain("oitenta").doesNotContain("80");
        System.out.println(answer.answer());
    }

    @Test
    void should_rememberPreviousMessage_when_sameConversation() {
        assistantService.chat(bruno, "memoria", "Gastei 55 reais no mercado hoje");

        var answer = assistantService.chat(bruno, "memoria", "Quanto foi mesmo o valor que eu acabei de falar?");

        assertThat(answer.answer().toLowerCase()).containsAnyOf("cinquenta e cinco", "55");
        System.out.println(answer.answer());
    }

    @Test
    void should_warnAboutBudget_when_expenseCrossesTheLimit() {
        budgetService.upsert(bruno, new BudgetRequest(Category.GROCERIES, new BigDecimal("100"), null));

        var answer = assistantService.chat(bruno, null, "Gastei 95 reais no mercado hoje");

        assertThat(answer.answer().toLowerCase()).containsAnyOf("limite", "orçamento", "por cento", "%");
        System.out.println(answer.answer());
    }

    @Test
    void should_explainThatSpeechIsDisabled_when_mp3IsRequested() throws IOException {
        var audio = new ClassPathResource("audio/recording-1.m4a");
        var file = new MockMultipartFile("file", "recording-1.m4a", "audio/m4a", audio.getInputStream());

        assertThatThrownBy(() -> assistantService.voiceToVoice(bruno, null, file))
                .isInstanceOf(FeatureUnavailableException.class)
                .hasMessageContaining("/assistant/voice/text");
    }
}
