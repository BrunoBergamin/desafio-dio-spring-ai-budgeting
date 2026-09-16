package dio.budgeting.service;

import dio.budgeting.dto.request.LoginRequest;
import dio.budgeting.dto.request.RegisterRequest;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.InvalidCredentialsException;
import dio.budgeting.mapper.UserMapper;
import dio.budgeting.repository.UserRepository;
import dio.budgeting.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, new UserMapper());
    }

    @Test
    void should_createUserAndReturnToken_when_emailIsNew() {
        when(userRepository.existsByEmail("bruno@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generate(any())).thenReturn("token-jwt");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        var response = service.register(new RegisterRequest("Bruno", "  Bruno@Email.com ", "senha-forte-123"));

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.user().email()).isEqualTo("bruno@email.com");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hash") && u.getEmail().equals("bruno@email.com")));
    }

    @Test
    void should_rejectRegistration_when_emailAlreadyExists() {
        when(userRepository.existsByEmail("bruno@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Bruno", "bruno@email.com", "senha-forte-123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já existe");
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_throwInvalidCredentials_when_passwordIsWrong() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("bruno@email.com", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void should_normalizeBrazilianPhone_when_linking() {
        assertThat(AuthService.normalizePhone("(19) 99999-9999")).isEqualTo("5519999999999");
        assertThat(AuthService.normalizePhone("+55 19 99999-9999")).isEqualTo("5519999999999");
        assertThat(AuthService.normalizePhone("1933334444")).isEqualTo("551933334444");
        assertThatThrownBy(() -> AuthService.normalizePhone("123")).isInstanceOf(BusinessException.class);
    }

    @Test
    void should_rejectPhone_when_alreadyLinkedToAnotherAccount() {
        var me = UUID.randomUUID();
        var other = new User("Outra", "outra@email.com", "hash");
        org.springframework.test.util.ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
        when(userRepository.findByPhone("5519999999999")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.linkPhone(me, "19 99999-9999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outra conta");
    }

    @Test
    void should_returnToken_when_loginIsValid() {
        var user = new User("Bruno", "bruno@email.com", "hash");
        when(userRepository.findByEmail("bruno@email.com")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("token-jwt");

        var response = service.login(new LoginRequest("BRUNO@email.com", "senha-forte-123"));

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
