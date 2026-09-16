package dio.budgeting.controller;

import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.FeatureUnavailableException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.AssistantService;
import dio.budgeting.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SecuredWebMvcTest(AssistantController.class)
class AssistantControllerTest {

    static final String USER_ID = "22222222-2222-2222-2222-222222222222";
    static final UUID USER = UUID.fromString(USER_ID);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AssistantService assistantService;

    @MockitoBean
    AppUserDetailsService userDetailsService;

    @Test
    void should_return401_when_tokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "oi"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_returnAnswer_when_textCommandIsSent() throws Exception {
        when(assistantService.chat(USER, "principal", "Gastei 80 reais no mercado"))
                .thenReturn(new AssistantResponse(null, "Registrei oitenta reais em mercado.", "principal"));

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Gastei 80 reais no mercado", "conversationId": "principal"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Registrei oitenta reais em mercado."))
                .andExpect(jsonPath("$.conversationId").value("principal"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_messageIsBlank() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return400_when_conversationIdHasInvalidCharacters() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "oi", "conversationId": "../outro usuario"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.conversationId").exists());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_returnTranscriptionAndAnswer_when_audioIsSent() throws Exception {
        when(assistantService.voiceToText(eq(USER), isNull(), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(new AssistantResponse("Gastei 80 reais no mercado.", "Registrei oitenta reais em mercado.", "default"));

        mockMvc.perform(multipart("/api/assistant/voice/text").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("Gastei 80 reais no mercado."));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return422_when_fileIsNotAudio() throws Exception {
        when(assistantService.voiceToText(eq(USER), isNull(), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new BusinessException("o arquivo enviado não é um áudio (text/plain)"));

        mockMvc.perform(multipart("/api/assistant/voice/text").file(audioFile()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return503_when_textToSpeechIsDisabled() throws Exception {
        when(assistantService.voiceToVoice(eq(USER), isNull(), any()))
                .thenThrow(new FeatureUnavailableException("a resposta em áudio não está habilitada neste perfil"));

        mockMvc.perform(multipart("/api/assistant/voice").file(audioFile()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Recurso indisponível"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_return204_when_conversationIsCleared() throws Exception {
        mockMvc.perform(delete("/api/assistant/conversation").param("conversationId", "principal"))
                .andExpect(status().isNoContent());

        verify(assistantService).forget(USER, "principal");
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "recording.m4a", "audio/m4a", "conteudo".getBytes());
    }
}
