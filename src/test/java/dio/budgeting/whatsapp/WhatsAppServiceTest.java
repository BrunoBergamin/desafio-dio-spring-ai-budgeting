package dio.budgeting.whatsapp;

import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.entity.User;
import dio.budgeting.repository.UserRepository;
import dio.budgeting.service.AssistantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    static final String PHONE = "5519999999999";

    @Mock WhatsAppGateway gateway;
    @Mock AssistantService assistantService;
    @Mock UserRepository userRepository;
    @Mock WhatsAppProperties properties;
    @Mock dio.budgeting.demo.DemoProperties demoProperties;
    @InjectMocks WhatsAppService service;

    @Test
    void should_linkPairedNumberToDemoAccount_onFirstSelfMessage_when_demoIsEnabled() {
        var demo = userWithId();
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(demoProperties.enabled()).thenReturn(true);
        when(demoProperties.email()).thenReturn("demo@lumi.local");
        when(userRepository.findByEmail("demo@lumi.local")).thenReturn(Optional.of(demo));
        when(userRepository.save(demo)).thenReturn(demo);
        when(assistantService.chat(demo.getId(), "whatsapp", "gastei 10 reais")).thenReturn(new AssistantResponse(null, "Registrei.", "whatsapp"));
        when(assistantService.speak(any())).thenReturn(Optional.empty());

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", true), text("gastei 10 reais")));

        assertThat(demo.getPhone()).isEqualTo(PHONE);
        verify(gateway).sendText(PHONE, "Registrei.");
    }

    static final String FRIEND = "5511888888888";

    @Test
    void should_ignoreMessagesISendToOtherPeople_GroupsAndOtherEvents() {
        // fromMe para um amigo: nao e o chat "Voce", a Lumi nao se mete
        assertThat(service.parse(payload("messages.upsert", key(FRIEND + "@s.whatsapp.net", true), text("oi")))).isEmpty();
        assertThat(service.parse(payload("messages.upsert", key("123@g.us", false), text("oi")))).isEmpty();
        assertThat(service.parse(payload("connection.update", key(PHONE + "@s.whatsapp.net", false), text("oi")))).isEmpty();
        assertThat(service.parse(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), Map.of("stickerMessage", Map.of())))).isEmpty();
    }

    @Test
    void should_extractPhoneAndText_when_messageIsPlainText() {
        var incoming = service.parse(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), text("gastei 10 reais"))).orElseThrow();

        assertThat(incoming.phone()).isEqualTo(PHONE);
        assertThat(incoming.text()).isEqualTo("gastei 10 reais");
        assertThat(incoming.audio()).isFalse();
    }

    @Test
    void should_acceptMessageToMyself_when_chatIsTheOwnerNumber() {
        // Chat "Voce": fromMe = true e remoteJid = o proprio numero pareado (sender do webhook)
        var incoming = service.parse(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", true), text("gastei 30 na farmácia"))).orElseThrow();

        assertThat(incoming.phone()).isEqualTo(PHONE);
        assertThat(incoming.text()).isEqualTo("gastei 30 na farmácia");
    }

    @Test
    void should_ignoreLumisOwnReply_when_itEchoesBackThroughTheWebhook() {
        var user = userWithId();
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(assistantService.chat(any(), any(), any())).thenReturn(new AssistantResponse(null, "Registrei.", "whatsapp"));
        when(assistantService.speak(any())).thenReturn(Optional.empty());
        when(gateway.sendText(PHONE, "Registrei.")).thenReturn("MSG-DA-LUMI");

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", true), text("gastei 10")));
        // ... e a propria resposta volta pelo webhook, com o mesmo id
        var echo = Map.<String, Object>of("remoteJid", PHONE + "@s.whatsapp.net", "fromMe", true, "id", "MSG-DA-LUMI");
        assertThat(service.parse(payload("messages.upsert", echo, text("Registrei.")))).isEmpty();

        verify(assistantService, times(1)).chat(any(), any(), any());
    }

    @Test
    void should_stripDeviceSuffix_when_readingTheOwnerJid() {
        assertThat(WhatsAppService.digits("5519999999999:12@s.whatsapp.net")).isEqualTo("5519999999999");
        assertThat(WhatsAppService.digits("5519999999999@s.whatsapp.net")).isEqualTo("5519999999999");
        assertThat(WhatsAppService.digits(null)).isNull();
    }

    @Test
    void should_usePhoneFromRemoteJidAlt_when_remoteJidIsLid() {
        var key = Map.<String, Object>of("remoteJid", "987654@lid", "remoteJidAlt", PHONE + "@s.whatsapp.net", "fromMe", false, "id", "X");

        var incoming = service.parse(payload("messages.upsert", key, text("oi"))).orElseThrow();

        assertThat(incoming.phone()).isEqualTo(PHONE);
    }

    @Test
    void should_stayQuiet_when_phoneIsNotLinked() {
        // Padrao: o WhatsApp pareado recebe mensagem de qualquer pessoa; estranhos nao recebem resposta
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(properties.replyUnknown()).thenReturn(false);

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), text("oi")));

        verifyNoInteractions(gateway, assistantService, demoProperties);
    }

    @Test
    void should_replyWithInstructions_when_phoneIsNotLinkedAndReplyIsEnabled() {
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(properties.replyUnknown()).thenReturn(true);

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), text("oi")));

        verify(gateway).sendText(eq(PHONE), contains("não está vinculado"));
        verifyNoInteractions(assistantService);
    }

    @Test
    void should_answerThroughLumi_when_textComesFromLinkedUser() {
        var user = userWithId();
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(assistantService.chat(user.getId(), "whatsapp", "gastei 10 reais no mercado"))
                .thenReturn(new AssistantResponse(null, "Registrei dez reais.", "whatsapp"));
        when(assistantService.speak("Registrei dez reais.")).thenReturn(Optional.empty());

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), text("gastei 10 reais no mercado")));

        verify(gateway).sendText(PHONE, "Registrei dez reais.");
        verify(gateway, never()).sendAudio(any(), any());
    }

    @Test
    void should_transcribeBase64Audio_when_webhookBringsIt() {
        var user = userWithId();
        var bytes = "ogg-bytes".getBytes();
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(assistantService.voiceToText(eq(user.getId()), eq("whatsapp"), any(AssistantService.AudioInput.class)))
                .thenReturn(new AssistantResponse("gastei 20", "Registrei vinte reais.", "whatsapp"));
        when(assistantService.speak(any())).thenReturn(Optional.of(new byte[] {1, 2, 3}));
        var message = Map.<String, Object>of(
                "audioMessage", Map.of("mimetype", "audio/ogg; codecs=opus"),
                "base64", Base64.getEncoder().encodeToString(bytes));

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), message));

        var captor = ArgumentCaptor.forClass(AssistantService.AudioInput.class);
        verify(assistantService).voiceToText(eq(user.getId()), eq("whatsapp"), captor.capture());
        assertThat(captor.getValue().bytes()).isEqualTo(bytes);
        assertThat(captor.getValue().filename()).isEqualTo("whatsapp.ogg");
        assertThat(captor.getValue().contentType()).isEqualTo("audio/ogg");
        verify(gateway).sendText(PHONE, "Registrei vinte reais.");
        verify(gateway).sendAudio(eq(PHONE), any());
        verify(gateway, never()).downloadMedia(any());
    }

    @Test
    void should_downloadMedia_when_webhookHasNoBase64() {
        var user = userWithId();
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(gateway.downloadMedia(any())).thenReturn(Optional.of(new WhatsAppGateway.Media("x".getBytes(), "audio/ogg")));
        when(assistantService.voiceToText(any(), any(), any(AssistantService.AudioInput.class)))
                .thenReturn(new AssistantResponse("oi", "Oi!", "whatsapp"));
        when(assistantService.speak(any())).thenReturn(Optional.empty());

        service.handle(payload("messages.upsert", key(PHONE + "@s.whatsapp.net", false), Map.of("audioMessage", Map.of("mimetype", "audio/ogg"))));

        verify(gateway).downloadMedia(any());
        verify(gateway).sendText(PHONE, "Oi!");
    }

    /** O webhook da Evolution traz em "sender" o numero do WhatsApp pareado (o dono da instancia). */
    private static Map<String, Object> payload(String event, Map<String, Object> key, Map<String, Object> message) {
        return Map.of("event", event, "instance", "lumi", "sender", PHONE + "@s.whatsapp.net",
                "data", Map.of("key", key, "pushName", "Bruno", "message", message));
    }

    private static Map<String, Object> key(String remoteJid, boolean fromMe) {
        return Map.of("remoteJid", remoteJid, "fromMe", fromMe, "id", "ABC");
    }

    private static Map<String, Object> text(String value) {
        return Map.of("conversation", value);
    }

    private static User userWithId() {
        var user = new User("Bruno", "bruno@email.com", "hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
