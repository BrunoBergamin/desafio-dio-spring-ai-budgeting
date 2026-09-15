package dio.budgeting.controller;

import dio.budgeting.dto.request.ChatRequest;
import dio.budgeting.dto.response.AssistantResponse;
import dio.budgeting.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Assistente IA", description = "Comandos em texto ou voz processados com Spring AI")
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @Operation(summary = "Envia um comando em texto (ex.: 'gastei 50 reais na farmácia')")
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistantService.chat(request.message());
    }

    @Operation(summary = "Envia um áudio e recebe a transcrição e a resposta em texto (JSON)")
    @PostMapping(value = "/voice/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssistantResponse voiceToText(@RequestParam("file") MultipartFile file) {
        return assistantService.voiceToText(file);
    }

    @Operation(summary = "Envia um áudio e recebe a resposta falada em MP3 (fluxo principal)")
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mpeg")
    public ResponseEntity<Resource> voiceToVoice(@RequestParam("file") MultipartFile file) {
        var audio = assistantService.voiceToVoice(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resposta.mp3").build().toString())
                .body(new ByteArrayResource(audio));
    }
}
