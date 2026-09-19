package dio.budgeting.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o agendador do Spring. Fica separado e condicionado a uma propriedade para os testes de contexto
 * nao dispararem o gerador de contas recorrentes.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.recurring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
