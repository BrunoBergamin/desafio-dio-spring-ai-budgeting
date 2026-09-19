package dio.budgeting.support;

import dio.budgeting.config.JwtConfig;
import dio.budgeting.config.SecurityConfig;
import dio.budgeting.security.CookieOrBearerTokenResolver;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.security.ProblemDetailResponses;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link WebMvcTest} nao carrega a SecurityConfig do projeto (so a cadeia padrao do Boot, que devolve 401
 * sem corpo). Esta anotacao importa a configuracao real, para os testes verem o mesmo 401/403 da aplicacao.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@WebMvcTest
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailResponses.class, CurrentUserProvider.class,
        CookieOrBearerTokenResolver.class})
public @interface SecuredWebMvcTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
