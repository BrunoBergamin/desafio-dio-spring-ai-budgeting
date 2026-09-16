package dio.budgeting.service;

import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.FeatureUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    ChatClient chatClient;

    @Mock
    TranscriptionModel transcriptionModel;

    @Mock
    ObjectProvider<TextToSpeechModel> textToSpeechProvider;

    AssistantService service;

    @BeforeEach
    void setUp() {
        Resource prompt = new ByteArrayResource("Você é um assistente. Hoje é {today}.".getBytes());
        service = new AssistantService(chatClient, transcriptionModel, textToSpeechProvider, prompt);
    }

    @Test
    void should_rejectEmptyFile() {
        var empty = new MockMultipartFile("file", "vazio.m4a", "audio/m4a", new byte[0]);

        assertThatThrownBy(() -> service.voiceToText(empty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivo de áudio");
    }

    @Test
    void should_rejectFileThatIsNotAudio() {
        var pdf = new MockMultipartFile("file", "extrato.pdf", "application/pdf", "conteudo".getBytes());

        assertThatThrownBy(() -> service.voiceToText(pdf))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não é um áudio");
    }

    @Test
    void should_explainFormat_when_providerRejectsTheAudio() {
        when(transcriptionModel.transcribe(any()))
                .thenThrow(new RuntimeException("400: file must be one of the following types: [flac mp3 m4a wav]"));

        assertThatThrownBy(() -> service.voiceToText(audio()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("converta para mp3 ou wav");
    }

    @Test
    void should_rethrow_when_errorIsNotAboutTheFormat() {
        when(transcriptionModel.transcribe(any()))
                .thenThrow(new IllegalStateException("401: invalid api key"));

        assertThatThrownBy(() -> service.voiceToText(audio()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_failFast_when_speechIsDisabled() {
        when(textToSpeechProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.voiceToVoice(audio()))
                .isInstanceOf(FeatureUnavailableException.class)
                .hasMessageContaining("/assistant/voice/text");
    }

    @Test
    void should_rejectBlankTranscription() {
        when(transcriptionModel.transcribe(any())).thenReturn("   ");

        assertThatThrownBy(() -> service.voiceToText(audio()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não foi possível entender o áudio");
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("file", "gravacao.m4a", "audio/m4a", "bytes-de-audio".getBytes());
    }
}
