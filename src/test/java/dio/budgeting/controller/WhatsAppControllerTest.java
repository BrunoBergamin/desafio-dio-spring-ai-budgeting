package dio.budgeting.controller;

import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.support.SecuredWebMvcTest;
import dio.budgeting.whatsapp.WhatsAppGateway;
import dio.budgeting.whatsapp.WhatsAppProperties;
import dio.budgeting.whatsapp.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SecuredWebMvcTest(WhatsAppController.class)
@TestPropertySource(properties = "app.whatsapp.enabled=true")
class WhatsAppControllerTest {

    static final String SECRET = "segredo-do-webhook-com-16-ou-mais";

    @Autowired MockMvc mockMvc;
    @MockitoBean WhatsAppService whatsAppService;
    @MockitoBean WhatsAppGateway gateway;
    @MockitoBean WhatsAppProperties properties;
    @MockitoBean AppUserDetailsService userDetailsService;

    @Test
    void should_accept_when_webhookSecretMatches() throws Exception {
        when(properties.webhookSecret()).thenReturn(SECRET);

        mockMvc.perform(post("/api/whatsapp/webhook/{secret}", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"messages.upsert","data":{"key":{"remoteJid":"5519999999999@s.whatsapp.net","fromMe":false},"message":{"conversation":"oi"}}}
                                """))
                .andExpect(status().isAccepted());

        verify(whatsAppService).handle(any());
    }

    @Test
    void should_return404AndIgnore_when_webhookSecretIsWrong() throws Exception {
        when(properties.webhookSecret()).thenReturn(SECRET);

        mockMvc.perform(post("/api/whatsapp/webhook/{secret}", "errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages.upsert\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(whatsAppService);
    }

    @Test
    void should_requireLogin_forStatusAndConnect() throws Exception {
        mockMvc.perform(get("/api/whatsapp/status")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/whatsapp/connect")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "66666666-6666-6666-6666-666666666666")
    void should_returnQrCode_when_connecting() throws Exception {
        when(gateway.connect()).thenReturn(new WhatsAppGateway.ConnectionInfo("connecting", "data:image/png;base64,AAA", null));

        mockMvc.perform(post("/api/whatsapp/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("connecting"))
                .andExpect(jsonPath("$.qrCodeBase64").value("data:image/png;base64,AAA"))
                .andExpect(jsonPath("$.connected").value(false));
    }
}
