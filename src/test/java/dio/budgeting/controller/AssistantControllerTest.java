package dio.budgeting.controller;

import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.FeatureUnavailableException;
import dio.budgeting.service.AssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AssistantService assistantService;

    @Test
    void should_returnAnswer_when_textCommandIsSent() throws Exception {
        when(assistantService.chat("Gastei 80 reais no mercado"))
                .thenReturn(new AssistantResponse(null, "Registrei oitenta reais em mercado."));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Gastei 80 reais no mercado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Registrei oitenta reais em mercado."));
    }

    @Test
    void should_return400_when_messageIsBlank() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    void should_returnTranscriptionAndAnswer_when_audioIsSent() throws Exception {
        when(assistantService.voiceToText(any()))
                .thenReturn(new AssistantResponse("Gastei 80 reais no mercado.", "Registrei oitenta reais em mercado."));

        mockMvc.perform(multipart("/assistant/voice/text").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("Gastei 80 reais no mercado."));
    }

    @Test
    void should_return422_when_fileIsNotAudio() throws Exception {
        when(assistantService.voiceToText(any()))
                .thenThrow(new BusinessException("o arquivo enviado não é um áudio (text/plain)"));

        mockMvc.perform(multipart("/assistant/voice/text").file(audioFile()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void should_return503_when_textToSpeechIsDisabled() throws Exception {
        when(assistantService.voiceToVoice(any()))
                .thenThrow(new FeatureUnavailableException("a resposta em áudio não está habilitada neste perfil"));

        mockMvc.perform(multipart("/assistant/voice").file(audioFile()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Recurso indisponível"));
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "recording.m4a", "audio/m4a", "conteudo".getBytes());
    }
}
