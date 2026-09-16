package dio.budgeting.security;

import dio.budgeting.config.JwtProperties;
import dio.budgeting.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    /** O subject e o id do usuario: e dele que o resto da aplicacao deriva o escopo dos dados. */
    public String generate(User user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("controle-financeiro")
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .subject(user.getId().toString())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .build();

        // Com chave simetrica o header precisa ser explicito, senao o encoder assume RS256
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }
}
