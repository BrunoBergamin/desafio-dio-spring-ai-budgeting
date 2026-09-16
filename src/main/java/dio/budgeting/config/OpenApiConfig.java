package dio.budgeting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI budgetingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Controle Financeiro - API da assistente Lumi")
                .description("Registre e consulte seus gastos por REST, por texto ou falando com a Lumi.")
                .version("1.0.0"));
    }
}
