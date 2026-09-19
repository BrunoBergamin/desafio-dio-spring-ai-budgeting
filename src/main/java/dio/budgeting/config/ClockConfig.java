package dio.budgeting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relogio unico da aplicacao, no fuso do usuario (app.timezone, padrao America/Sao_Paulo).
 * <p>
 * Sem isso, {@code LocalDate.now()} usa o fuso do servidor: dentro do container (UTC) um gasto
 * registrado as 22h de Brasilia cairia no dia seguinte, e no dia 30 ou 31 o "mes atual" viraria
 * o mes que vem. Injetar o {@link Clock} nos services tambem deixa os testes de data
 * deterministicos ({@code Clock.fixed}).
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
