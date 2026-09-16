package dio.budgeting.controller;

import dio.budgeting.whatsapp.WhatsAppGateway;
import dio.budgeting.whatsapp.WhatsAppProperties;
import dio.budgeting.whatsapp.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Lado HTTP da integracao com o WhatsApp: o webhook que a Evolution chama e as rotas
 * que o frontend usa para parear o numero (QR code) e ver o estado da conexao.
 */
@Slf4j
@Tag(name = "WhatsApp", description = "Falar com a Lumi pelo WhatsApp (Evolution API)")
@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.whatsapp", name = "enabled", havingValue = "true")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppGateway gateway;
    private final WhatsAppProperties properties;

    @Operation(summary = "Webhook chamado pela Evolution API a cada mensagem (protegido pelo segredo na URL)")
    @PostMapping("/webhook/{secret}")
    public ResponseEntity<Void> webhook(@PathVariable String secret, @RequestBody Map<String, Object> payload) {
        if (!constantTimeEquals(secret, properties.webhookSecret())) {
            // 404 e nao 403: nao confirma que a rota existe para quem chuta a URL
            return ResponseEntity.notFound().build();
        }
        whatsAppService.handle(payload);
        return ResponseEntity.accepted().build(); // 202: processa em segundo plano
    }

    @Operation(summary = "Estado da conexão com o WhatsApp")
    @GetMapping("/status")
    public WhatsAppGateway.ConnectionInfo status() {
        return gateway.status();
    }

    @Operation(summary = "Cria a instância (se preciso) e devolve o QR code para parear o número")
    @PostMapping("/connect")
    @ResponseStatus(HttpStatus.OK)
    public WhatsAppGateway.ConnectionInfo connect() {
        return gateway.connect();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
