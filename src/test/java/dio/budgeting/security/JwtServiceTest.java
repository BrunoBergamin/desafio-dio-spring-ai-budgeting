package dio.budgeting.security;

import dio.budgeting.config.JwtProperties;
import dio.budgeting.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static final JwtProperties PROPS = new JwtProperties("segredo-de-teste-com-pelo-menos-32-caracteres!!", Duration.ofHours(1));

    JwtService service = new JwtService(
            NimbusJwtEncoder.withSecretKey(PROPS.secretKey()).algorithm(MacAlgorithm.HS256).build(), PROPS);

    @Test
    void should_useUserIdAsSubject_when_tokenIsGenerated() {
        var user = userWithId(UUID.randomUUID());

        var token = service.generate(user);
        var decoded = NimbusJwtDecoder.withSecretKey(PROPS.secretKey()).macAlgorithm(MacAlgorithm.HS256).build().decode(token);

        assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getClaimAsString("email")).isEqualTo("bruno@email.com");
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
    }

    @Test
    void should_rejectToken_when_secretIsDifferent() {
        var token = service.generate(userWithId(UUID.randomUUID()));
        var otherKey = new JwtProperties("outro-segredo-completamente-diferente-e-longo!!", Duration.ofHours(1)).secretKey();
        var decoder = NimbusJwtDecoder.withSecretKey(otherKey).macAlgorithm(MacAlgorithm.HS256).build();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void should_rejectShortSecret_when_propertiesAreCreated() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private User userWithId(UUID id) {
        var user = new User("Bruno", "bruno@email.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
