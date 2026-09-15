package dio.budgeting.service;

import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.FeatureUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Orquestra o fluxo de IA: áudio -> texto -> ChatClient (com Tool Calling) -> texto -> áudio.
 */
@Slf4j
@Service
public class AssistantService {

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)", Locale.forLanguageTag("pt-BR"));

    private final ChatClient chatClient;
    private final TranscriptionModel transcriptionModel;
    private final ObjectProvider<TextToSpeechModel> textToSpeechModel;
    private final Resource systemPrompt;

    /**
     * O text-to-speech é opcional: perfis sem um provedor de voz (ex.: groq) continuam
     * atendendo as rotas de texto, em vez de impedir a aplicação de subir.
     */
    public AssistantService(ChatClient chatClient,
                            TranscriptionModel transcriptionModel,
                            ObjectProvider<TextToSpeechModel> textToSpeechModel,
                            @Value("classpath:prompts/system-message.st") Resource systemPrompt) {
        this.chatClient = chatClient;
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.systemPrompt = systemPrompt;
    }

    /** Texto -> resposta em texto. Útil para testar o Tool Calling sem precisar gravar áudio. */
    public AssistantResponse chat(String message) {
        return new AssistantResponse(null, ask(message));
    }

    /** Áudio -> transcrição + resposta em texto. */
    public AssistantResponse voiceToText(MultipartFile audio) {
        var transcription = transcribe(audio);
        return new AssistantResponse(transcription, ask(transcription));
    }

    /** Áudio -> resposta em áudio MP3 (fluxo principal do desafio). */
    public byte[] voiceToVoice(MultipartFile audio) {
        var speech = textToSpeechModel.getIfAvailable();
        if (speech == null) {
            throw new FeatureUnavailableException(
                    "a resposta em áudio não está habilitada neste perfil; use /assistant/voice/text");
        }
        var answer = voiceToText(audio).answer();
        return speech.call(answer);
    }

    private String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException("envie um arquivo de áudio no campo 'file'");
        }
        var contentType = audio.getContentType();
        if (contentType != null && !contentType.startsWith("audio/") && !contentType.startsWith("video/")
                && !contentType.equals("application/octet-stream")) {
            throw new BusinessException("o arquivo enviado não é um áudio (%s)".formatted(contentType));
        }

        var transcription = transcriptionModel.transcribe(audio.getResource());
        log.info("[assistant] transcrição: '{}'", transcription);

        if (transcription == null || transcription.isBlank()) {
            throw new BusinessException("não foi possível entender o áudio, tente gravar novamente");
        }
        return transcription.trim();
    }

    private String ask(String message) {
        var answer = chatClient.prompt()
                .system(system -> system.text(systemPrompt).param("today", LocalDate.now().format(TODAY_FORMAT)))
                .user(message)
                .call()
                .content();
        log.info("[assistant] pergunta: '{}' | resposta: '{}'", message, answer);
        return answer;
    }
}
