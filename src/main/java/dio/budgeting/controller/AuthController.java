package dio.budgeting.controller;

import dio.budgeting.dto.request.LoginRequest;
import dio.budgeting.dto.request.RegisterRequest;
import dio.budgeting.dto.response.AuthResponse;
import dio.budgeting.dto.response.UserResponse;
import dio.budgeting.security.AuthCookies;
import dio.budgeting.security.CurrentUserProvider;
import dio.budgeting.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Cadastro e login (JWT)")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUser;

    @Operation(summary = "Cria uma conta e já devolve o token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "422", description = "E-mail já cadastrado", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return withCookie(authService.register(request), httpRequest, httpResponse);
    }

    @Operation(summary = "Entra na conta de demonstração, sem senha (só com APP_DEMO_ENABLED=true)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token da conta demo"),
            @ApiResponse(responseCode = "404", description = "Modo demo desligado", content = @Content)
    })
    @PostMapping("/demo")
    public AuthResponse demo(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return withCookie(authService.demoLogin(), httpRequest, httpResponse);
    }

    @Operation(summary = "Entra com e-mail e senha e recebe o token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return withCookie(authService.login(request), httpRequest, httpResponse);
    }

    @Operation(summary = "Sai da conta: apaga o cookie do token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, AuthCookies.clear(httpRequest).toString());
    }

    /**
     * Devolve o token no corpo, para Swagger e curl, e tambem no cookie HttpOnly, que e o que o site usa.
     * Assim o JavaScript da pagina nunca precisa tocar no token.
     */
    private AuthResponse withCookie(AuthResponse auth, HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                AuthCookies.issue(auth.token(), auth.expiresIn(), request).toString());
        return auth;
    }

    public record PhoneRequest(
            @jakarta.validation.constraints.NotBlank(message = "informe o número do WhatsApp") String phone) {
    }

    @Operation(summary = "Vincula o número do WhatsApp à conta (para falar com a Lumi pelo WhatsApp)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Número vinculado"),
            @ApiResponse(responseCode = "422", description = "Número inválido ou já usado por outra conta", content = @Content)
    })
    @PutMapping("/me/phone")
    public UserResponse linkPhone(@Valid @RequestBody PhoneRequest request) {
        return authService.linkPhone(currentUser.requireUserId(), request.phone());
    }

    @Operation(summary = "Dados do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public UserResponse me() {
        return authService.me(currentUser.requireUserId());
    }
}
