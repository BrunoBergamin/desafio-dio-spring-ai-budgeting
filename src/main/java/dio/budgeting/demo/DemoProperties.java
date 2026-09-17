package dio.budgeting.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Modo demonstracao: a aplicacao cria uma conta pronta, com gastos e orcamentos de exemplo,
 * e o site entra nela sozinho. Quem for testar (voce, um recrutador, um professor) nao precisa
 * criar conta nem digitar senha. Desligue com APP_DEMO_ENABLED=false para uso real.
 */
@ConfigurationProperties("app.demo")
public record DemoProperties(boolean enabled, String email, String password, String name) {

    public DemoProperties {
        if (email == null || email.isBlank()) {
            email = "demo@lumi.local";
        }
        if (password == null || password.length() < 8) {
            password = "lumi-demo-1234";
        }
        if (name == null || name.isBlank()) {
            name = "Bruno (demo)";
        }
    }
}
