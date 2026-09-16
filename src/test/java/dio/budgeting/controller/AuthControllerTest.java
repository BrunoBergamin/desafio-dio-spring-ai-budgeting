package dio.budgeting.controller;

import dio.budgeting.dto.response.AuthResponse;
import dio.budgeting.dto.response.UserResponse;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.InvalidCredentialsException;
import dio.budgeting.security.AppUserDetailsService;
import dio.budgeting.service.AuthService;
import dio.budgeting.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SecuredWebMvcTest(AuthController.class)
class AuthControllerTest {

    static final String USER_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    AppUserDetailsService userDetailsService;

    @Test
    void should_return201WithToken_when_registrationIsValid() throws Exception {
        when(authService.register(any())).thenReturn(AuthResponse.bearer("token-jwt", 28800,
                new UserResponse(UUID.fromString(USER_ID), "Bruno", "bruno@email.com")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bruno", "email": "bruno@email.com", "password": "senha-forte-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.name").value("Bruno"));
    }

    @Test
    void should_return400_when_passwordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bruno", "email": "bruno@email.com", "password": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void should_return422_when_emailAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessException("já existe uma conta com esse e-mail"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bruno", "email": "bruno@email.com", "password": "senha-forte-123"}
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void should_return401_when_credentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "bruno@email.com", "password": "errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais inválidas"));
    }

    @Test
    void should_return401_when_meIsCalledWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void should_returnCurrentUser_when_authenticated() throws Exception {
        when(authService.me(UUID.fromString(USER_ID)))
                .thenReturn(new UserResponse(UUID.fromString(USER_ID), "Bruno", "bruno@email.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bruno@email.com"));
    }
}
