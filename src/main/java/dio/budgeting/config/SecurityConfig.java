package dio.budgeting.config;

import dio.budgeting.security.ProblemDetailResponses;
import dio.budgeting.security.RateLimitFilter;
import dio.budgeting.security.RateLimitProperties;
import dio.budgeting.web.SpaRoutes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                    ProblemDetailResponses problemResponses,
                                    RateLimitProperties rateLimit,
                                    @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/demo").permitAll()
                            // O webhook da Evolution nao tem JWT: e protegido pelo segredo na URL
                            .requestMatchers(HttpMethod.POST, "/api/whatsapp/webhook/**").permitAll()
                            .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                            // Rotas do React: o index.html precisa carregar antes do login
                            .requestMatchers(HttpMethod.GET, SpaRoutes.PATHS).permitAll()
                            .requestMatchers(HttpMethod.GET, "/assets/**", "/favicon.ico", "/favicon.svg").permitAll();
                    if (h2ConsoleEnabled) {
                        // So no perfil dev: o console do H2 nao existe em producao nem com MySQL
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(problemResponses))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(problemResponses)
                        .accessDeniedHandler(problemResponses));

        if (h2ConsoleEnabled) {
            // O console do H2 abre em iframe; fora do dev a protecao contra clickjacking fica ligada
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
        if (rateLimit.enabled()) {
            // Depois do filtro do JWT (para contar por usuario) e antes da autorizacao
            http.addFilterBefore(new RateLimitFilter(rateLimit, problemResponses), AuthorizationFilter.class);
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
