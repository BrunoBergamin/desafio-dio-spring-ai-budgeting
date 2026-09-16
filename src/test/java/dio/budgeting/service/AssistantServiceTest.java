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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    static final UUID USER = UUID.randomUUID();

    @Mock
    LumiChat lumiChat;

    @Mock
    TranscriptionModel transcriptionModel;

    @Mock
    ObjectProvider<TextToSpeechModel> textToSpeechProvider;

    AssistantService service;

    @BeforeEach
    void setUp() {
        service = new AssistantService(lumiChat, transcriptionModel, textToSpeechProvider);
    }

    @Test
    void should_scopeConversationToTheUser_when_chatting() {
        when(lumiChat.answer(USER, USER + ":principal", "oi")).thenReturn("Oi!");

        var response = service.chat(USER, "principal", "oi");

        assertThat(response.answer()).isEqualTo("Oi!");
        assertThat(response.conversationId()).isEqualTo("principal");
    }

    @Test
    void should_useDefaultConversation_when_idIsMissing() {
        when(lumiChat.answer(USER, USER + ":default", "oi")).thenReturn("Oi!");

        assertThat(service.chat(USER, null, "oi").conversationId()).isEqualTo("default");
    }

    @Test
    void should_isolateConversations_when_usersAreDifferent() {
        var other = UUID.randomUUID();
        when(lumiChat.answer(any(), any(), any())).thenReturn("ok");

        service.chat(USER, "x", "oi");
        service.chat(other, "x", "oi");

        verify(lumiChat).answer(USER, USER + ":x", "oi");
        verify(lumiChat).answer(other, other + ":x", "oi");
    }

    @Test
    void should_rejectEmptyFile() {
        var empty = new MockMultipartFile("file", "vazio.m4a", "audio/m4a", new byte[0]);

        assertThatThrownBy(() -> service.voiceToText(USER, null, empty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivo de áudio");
    }

    @Test
    void should_rejectFileThatIsNotAudio() {
        var pdf = new MockMultipartFile("file", "extrato.pdf", "application/pdf", "conteudo".getBytes());

        assertThatThrownBy(() -> service.voiceToText(USER, null, pdf))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não é um áudio");
    }

    @Test
    void should_explainFormat_when_providerRejectsTheAudio() {
        when(transcriptionModel.transcribe(any()))
                .thenThrow(new RuntimeException("400: file must be one of the following types: [flac mp3 m4a wav]"));

        assertThatThrownBy(() -> service.voiceToText(USER, null, audio()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("converta para mp3 ou wav");
    }

    @Test
    void should_rethrow_when_errorIsNotAboutTheFormat() {
        when(transcriptionModel.transcribe(any())).thenThrow(new IllegalStateException("401: invalid api key"));

        assertThatThrownBy(() -> service.voiceToText(USER, null, audio())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_failFast_when_speechIsDisabled() {
        when(textToSpeechProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.voiceToVoice(USER, null, audio()))
                .isInstanceOf(FeatureUnavailableException.class)
                .hasMessageContaining("/assistant/voice/text");
    }

    @Test
    void should_rejectBlankTranscription() {
        when(transcriptionModel.transcribe(any())).thenReturn("   ");

        assertThatThrownBy(() -> service.voiceToText(USER, null, audio()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não foi possível entender o áudio");
    }

    @Test
    void should_sendTranscriptionToLumi_when_audioIsValid() {
        when(transcriptionModel.transcribe(any())).thenReturn(" Gastei 80 reais no mercado. ");
        when(lumiChat.answer(USER, USER + ":default", "Gastei 80 reais no mercado.")).thenReturn("Registrei.");

        var response = service.voiceToText(USER, null, audio());

        assertThat(response.transcription()).isEqualTo("Gastei 80 reais no mercado.");
        assertThat(response.answer()).isEqualTo("Registrei.");
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("file", "gravacao.m4a", "audio/m4a", "bytes-de-audio".getBytes());
    }
}
