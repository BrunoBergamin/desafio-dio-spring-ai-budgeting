package dio.budgeting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI budgetingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Controle Financeiro - API da assistente Lumi")
                        .description("Registre e consulte seus gastos por REST, por texto ou falando com a Lumi. "
                                + "Cadastre-se em /api/auth/register, clique em Authorize e cole o token.")
                        .version("2.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        // Quem já entrou no site tem o cookie e o "Try it out" funciona sem colar token
                        .addSecuritySchemes("cookieAuth",
                                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE).name("lumi_token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
