package dio.budgeting.support;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link DataJpaTest} nao carrega o Flyway. Como o schema vem das migrations (ddl-auto=validate),
 * esta anotacao garante que os testes de repositorio rodem sobre o mesmo schema da aplicacao.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public @interface JpaTest {
}
