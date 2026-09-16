package dio.budgeting.service;

import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.FeatureUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra o fluxo de IA: áudio -> texto -> Lumi (com Tool Calling e memória) -> texto -> áudio.
 */
@Slf4j
@Service
public class AssistantService {

    private final LumiChat lumiChat;
    private final TranscriptionModel transcriptionModel;
    private final ObjectProvider<TextToSpeechModel> textToSpeechModel;

    /**
     * O text-to-speech é opcional: perfis sem um provedor de voz (ex.: groq) continuam
     * atendendo as rotas de texto, em vez de impedir a aplicação de subir.
     */
    public AssistantService(LumiChat lumiChat,
                            TranscriptionModel transcriptionModel,
                            ObjectProvider<TextToSpeechModel> textToSpeechModel) {
        this.lumiChat = lumiChat;
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    /** Texto -> resposta em texto. Útil para testar o Tool Calling sem precisar gravar áudio. */
    public AssistantResponse chat(UUID userId, String conversationId, String message) {
        var slug = ConversationKey.slug(conversationId);
        return new AssistantResponse(null, lumiChat.answer(userId, ConversationKey.of(userId, slug), message), slug);
    }

    /** Áudio -> transcrição + resposta em texto. */
    public AssistantResponse voiceToText(UUID userId, String conversationId, MultipartFile audio) {
        var slug = ConversationKey.slug(conversationId);
        var transcription = transcribe(audio);
        var answer = lumiChat.answer(userId, ConversationKey.of(userId, slug), transcription);
        return new AssistantResponse(transcription, answer, slug);
    }

    /** Áudio -> resposta em áudio MP3 (fluxo principal do desafio). */
    public byte[] voiceToVoice(UUID userId, String conversationId, MultipartFile audio) {
        var speech = textToSpeechModel.getIfAvailable();
        if (speech == null) {
            throw new FeatureUnavailableException(
                    "a resposta em áudio não está habilitada neste perfil; use /assistant/voice/text");
        }
        var answer = voiceToText(userId, conversationId, audio).answer();
        return speech.call(answer);
    }

    /**
     * Áudio que não veio por upload HTTP (ex.: WhatsApp): bytes + nome com extensão + content-type.
     * O nome importa: o serviço de transcrição decide o formato pela extensão.
     */
    public record AudioInput(byte[] bytes, String filename, String contentType) {
    }

    public AssistantResponse voiceToText(UUID userId, String conversationId, AudioInput audio) {
        var resource = new ByteArrayResource(audio.bytes()) {
            @Override
            public String getFilename() {
                return audio.filename();
            }
        };
        var slug = ConversationKey.slug(conversationId);
        var transcription = transcribe(resource, audio.contentType());
        var answer = lumiChat.answer(userId, ConversationKey.of(userId, slug), transcription);
        return new AssistantResponse(transcription, answer, slug);
    }

    /** Converte a resposta em MP3 quando o perfil tem text-to-speech; vazio quando não tem. */
    public Optional<byte[]> speak(String text) {
        var speech = textToSpeechModel.getIfAvailable();
        return speech == null ? Optional.empty() : Optional.of(speech.call(text));
    }

    /** Apaga o histórico da conversa (botão "nova conversa"). */
    public void forget(UUID userId, String conversationId) {
        lumiChat.forget(ConversationKey.of(userId, ConversationKey.slug(conversationId)));
    }

    private String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException("envie um arquivo de áudio no campo 'file'");
        }
        return transcribe(audio.getResource(), audio.getContentType());
    }

    private String transcribe(Resource audio, String contentType) {
        if (contentType != null && !contentType.startsWith("audio/") && !contentType.startsWith("video/")
                && !contentType.equals("application/octet-stream")) {
            throw new BusinessException("o arquivo enviado não é um áudio (%s)".formatted(contentType));
        }

        String transcription;
        try {
            transcription = transcriptionModel.transcribe(audio);
        } catch (RuntimeException e) {
            if (isUnsupportedAudio(e)) {
                // Acontece, por exemplo, com gravações do Windows salvas como AAC cru dentro de um .m4a
                throw new BusinessException(
                        "o formato deste áudio não é aceito pelo serviço de transcrição; "
                                + "converta para mp3 ou wav e envie novamente");
            }
            throw e;
        }
        log.info("[assistant] transcrição: '{}'", transcription);

        if (transcription == null || transcription.isBlank()) {
            throw new BusinessException("não foi possível entender o áudio, tente gravar novamente");
        }
        return transcription.trim();
    }

    /** O provedor responde 400 quando o arquivo não é um áudio que ele consegue decodificar. */
    private boolean isUnsupportedAudio(RuntimeException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            var message = t.getMessage();
            if (message != null
                    && (message.contains("file must be one of") || message.contains("unsupported_audio_format"))) {
                return true;
            }
        }
        return false;
    }
}
