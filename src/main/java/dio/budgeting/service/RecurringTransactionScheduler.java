package dio.budgeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cria os lancamentos das contas recorrentes que venceram.
 * <p>
 * Roda todo dia logo depois da meia-noite e tambem quando a aplicacao sobe: assim, quem deixa o Docker
 * desligado por uma semana nao perde nenhum mes, porque o gerador e por mes vencido e nao por "dia que passou".
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.recurring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecurringTransactionScheduler {

    private final RecurringTransactionService service;

    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone:America/Sao_Paulo}")
    public void daily() {
        service.generateDue();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        var created = service.generateDue();
        log.info("[recorrente] verificação na subida concluída ({} lançamento(s) criados)", created);
    }
}
