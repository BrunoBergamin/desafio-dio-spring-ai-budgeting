package dio.budgeting;

import dio.budgeting.entity.Category;
import dio.budgeting.exception.FeatureUnavailableException;
import dio.budgeting.repository.TransactionRepository;
import dio.budgeting.service.AssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mesmo fluxo do {@link AssistantFlowIT}, porém no perfil gratuito (Groq).
 * Só roda quando a variável GROQ_API_KEY está definida.
 */
@SpringBootTest
@ActiveProfiles("groq")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = "gsk_.+")
class AssistantFlowGroqIT {

    @Autowired
    AssistantService assistantService;

    @Autowired
    TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
    }

    @Test
    void should_persistTransaction_when_textCommandIsSent() {
        var response = assistantService.chat("Gastei 42 reais e 90 centavos na farmácia comprando remédio");

        var saved = transactionRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getAmount()).isEqualByComparingTo("42.90");
        assertThat(saved.getFirst().getCategory()).isEqualTo(Category.PHARMA);
        System.out.println(response.answer());
    }

    @Test
    void should_transcribeAndAnswer_when_audioIsSent() throws IOException {
        var audio = new ClassPathResource("audio/recording-1.m4a");
        var file = new MockMultipartFile("file", "recording-1.m4a", "audio/m4a", audio.getInputStream());

        var response = assistantService.voiceToText(file);

        assertThat(response.transcription()).containsIgnoringCase("reais");
        assertThat(transactionRepository.findAll()).hasSize(1);
        System.out.println(response.transcription() + " -> " + response.answer());
    }

    @Test
    void should_explainThatSpeechIsDisabled_when_mp3IsRequested() throws IOException {
        var audio = new ClassPathResource("audio/recording-1.m4a");
        var file = new MockMultipartFile("file", "recording-1.m4a", "audio/m4a", audio.getInputStream());

        assertThatThrownBy(() -> assistantService.voiceToVoice(file))
                .isInstanceOf(FeatureUnavailableException.class)
                .hasMessageContaining("/assistant/voice/text");
    }
}
