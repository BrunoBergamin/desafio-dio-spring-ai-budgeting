package dio.budgeting.whatsapp;

import dio.budgeting.repository.UserRepository;
import dio.budgeting.service.AssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Recebe a mensagem do WhatsApp, descobre de quem e, manda para a Lumi e responde.
 * A conversa fica na chave "whatsapp" da memoria, separada da conversa do site.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.whatsapp", name = "enabled", havingValue = "true")
public class WhatsAppService {

    static final String CONVERSATION = "whatsapp";
    static final String UNKNOWN_NUMBER_REPLY =
            "Oi! Eu sou a Lumi, assistente de controle de gastos. Esse número ainda não está vinculado a uma conta. "
                    + "Crie a sua conta no site e vincule este WhatsApp na página \"WhatsApp\" para eu registrar seus gastos.";

    private final WhatsAppGateway gateway;
    private final AssistantService assistantService;
    private final UserRepository userRepository;
    private final WhatsAppProperties properties;
    private final dio.budgeting.demo.DemoProperties demoProperties;

    /**
     * Ids das mensagens que a propria Lumi enviou. No chat "Voce" (mensagem para si mesmo) a resposta dela
     * tambem volta pelo webhook como fromMe: sem isso ela responderia a si mesma para sempre.
     */
    private final Set<String> sentMessageIds = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 500;
                }
            }));

    /** Roda fora da thread do webhook: a Evolution espera o 200 em poucos segundos e a Lumi demora mais. */
    @Async
    public void handle(Map<String, Object> payload) {
        try {
            parse(payload).ifPresent(this::reply);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            // Erro da Evolution (o caso mais comum: instancia criada mas ainda sem celular pareado)
            log.error("[whatsapp] a Evolution recusou o envio ({}). O WhatsApp está pareado? Veja /api/whatsapp/status. Resposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[whatsapp] falha ao processar mensagem", e);
        }
    }

    /** Extrai so o que interessa do evento messages.upsert; vazio para tudo que deve ser ignorado. */
    Optional<IncomingMessage> parse(Map<String, Object> payload) {
        if (!"messages.upsert".equals(str(payload.get("event")))) {
            return Optional.empty();
        }
        var data = asMap(payload.get("data"));
        var key = asMap(data.get("key"));
        var remoteJid = str(key.get("remoteJid"));
        if (remoteJid == null || remoteJid.endsWith("@g.us") || remoteJid.contains("broadcast")) {
            return Optional.empty(); // grupos e status
        }
        var phone = phoneFrom(key);
        if (phone == null) {
            log.warn("[whatsapp] mensagem sem número identificável: remoteJid={}", remoteJid);
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(key.get("fromMe"))) {
            // Mensagem enviada pelo proprio WhatsApp pareado. So interessa o chat "Voce" (mensagem para si mesmo):
            // o que voce manda para outras pessoas nao e da conta da Lumi.
            var owner = digits(str(payload.get("sender")));
            if (owner == null || !owner.equals(phone)) {
                return Optional.empty();
            }
            if (sentMessageIds.contains(str(key.get("id")))) {
                return Optional.empty(); // eco da resposta que a propria Lumi acabou de mandar
            }
        }
        var message = asMap(data.get("message"));
        var text = str(message.get("conversation"));
        if (text == null) {
            text = str(asMap(message.get("extendedTextMessage")).get("text"));
        }
        var audio = asMap(message.get("audioMessage"));
        if (text == null && audio.isEmpty()) {
            return Optional.empty(); // figurinha, imagem, etc.
        }
        var base64 = str(message.get("base64"));
        var selfChat = Boolean.TRUE.equals(key.get("fromMe"));
        return Optional.of(new IncomingMessage(phone, str(data.get("pushName")), text, !audio.isEmpty(),
                str(audio.get("mimetype")), base64, key, selfChat));
    }

    private void reply(IncomingMessage incoming) {
        var user = userRepository.findByPhone(incoming.phone());
        if (user.isEmpty() && incoming.selfChat() && demoProperties.enabled()) {
            // Modo demo: a primeira mensagem que voce manda para si mesmo vincula o numero pareado a conta demo
            user = userRepository.findByEmail(demoProperties.email()).map(demo -> {
                demo.linkPhone(incoming.phone());
                log.info("[whatsapp] número pareado vinculado à conta demo: {}", EvolutionApiGateway.mask(incoming.phone()));
                return userRepository.save(demo);
            });
        }
        if (user.isEmpty()) {
            // Quem nao esta vinculado e ignorado: o WhatsApp pareado recebe mensagens de qualquer pessoa,
            // e responder a estranhos so faz sentido com um numero dedicado a Lumi (app.whatsapp.reply-unknown=true)
            log.info("[whatsapp] mensagem de número não vinculado ignorada: {}", EvolutionApiGateway.mask(incoming.phone()));
            if (properties.replyUnknown()) {
                remember(gateway.sendText(incoming.phone(), UNKNOWN_NUMBER_REPLY));
            }
            return;
        }
        var userId = user.get().getId();
        String answer;
        if (incoming.audio()) {
            var bytes = incoming.base64() != null
                    ? Base64.getDecoder().decode(incoming.base64())
                    : gateway.downloadMedia(incoming.key()).map(WhatsAppGateway.Media::bytes).orElse(null);
            if (bytes == null) {
                remember(gateway.sendText(incoming.phone(), "Não consegui baixar o seu áudio. Pode mandar de novo?"));
                return;
            }
            // WhatsApp grava em ogg/opus, formato que o Whisper aceita direto
            var mimetype = incoming.mimetype() == null ? "audio/ogg" : incoming.mimetype().split(";")[0].trim();
            var input = new AssistantService.AudioInput(bytes, "whatsapp.ogg", mimetype);
            answer = assistantService.voiceToText(userId, CONVERSATION, input).answer();
        } else {
            answer = assistantService.chat(userId, CONVERSATION, incoming.text()).answer();
        }
        remember(gateway.sendText(incoming.phone(), answer));
        // No perfil OpenAI a Lumi tambem responde falando; na Groq nao ha text-to-speech
        assistantService.speak(answer).ifPresent(mp3 -> remember(gateway.sendAudio(incoming.phone(), mp3)));
    }

    private void remember(String messageId) {
        if (messageId != null) {
            sentMessageIds.add(messageId);
        }
    }

    /** "5519999999999@s.whatsapp.net" ou "5519999999999:12@s.whatsapp.net" -> "5519999999999". */
    static String digits(String jid) {
        if (jid == null || !jid.contains("@")) {
            return null;
        }
        var user = jid.substring(0, jid.indexOf('@'));
        var colon = user.indexOf(':');
        var value = (colon >= 0 ? user.substring(0, colon) : user).replaceAll("\\D", "");
        return value.isEmpty() ? null : value;
    }

    /** Numero do contato: "5519999999999@s.whatsapp.net" -> "5519999999999". Com LID, a Evolution manda o numero em remoteJidAlt. */
    static String phoneFrom(Map<String, Object> key) {
        for (var field : new String[] {"remoteJid", "remoteJidAlt", "senderPn", "participantAlt"}) {
            var jid = str(key.get(field));
            if (jid != null && jid.endsWith("@s.whatsapp.net")) {
                var digits = jid.substring(0, jid.indexOf('@')).replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    return digits;
                }
            }
        }
        return null;
    }

    record IncomingMessage(String phone, String pushName, String text, boolean audio, String mimetype,
                           String base64, Map<String, Object> key, boolean selfChat) {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
