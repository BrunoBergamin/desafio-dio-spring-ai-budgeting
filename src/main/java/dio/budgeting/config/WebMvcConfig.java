package dio.budgeting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Todos os controllers REST ficam sob /api. Assim o frontend, o Swagger e o Actuator
     * nao disputam rotas, e o proxy do Vite em desenvolvimento so precisa redirecionar /api.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackage("dio.budgeting.controller"));
    }
}
