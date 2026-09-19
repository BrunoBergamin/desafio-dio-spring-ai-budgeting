package dio.budgeting.service;

import dio.budgeting.dto.request.LoginRequest;
import dio.budgeting.dto.request.RegisterRequest;
import dio.budgeting.dto.response.AuthResponse;
import dio.budgeting.dto.response.UserResponse;
import dio.budgeting.entity.User;
import dio.budgeting.exception.BusinessException;
import dio.budgeting.exception.InvalidCredentialsException;
import dio.budgeting.exception.ResourceNotFoundException;
import dio.budgeting.mapper.UserMapper;
import dio.budgeting.repository.UserRepository;
import dio.budgeting.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final dio.budgeting.demo.DemoProperties demoProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        var email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("já existe uma conta com esse e-mail");
        }
        var user = userRepository.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        log.info("Usuário cadastrado: id={} email={}", user.getId(), user.getEmail());
        return issue(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var email = normalize(request.email());
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
        var user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        return issue(user);
    }

    /** Modo demo: entra na conta de demonstracao sem senha. Recusa (404) quando o modo esta desligado. */
    @Transactional(readOnly = true)
    public AuthResponse demoLogin() {
        if (!demoProperties.enabled()) {
            throw new ResourceNotFoundException("o modo demonstração está desligado");
        }
        var user = userRepository.findByEmail(demoProperties.email())
                .orElseThrow(() -> new ResourceNotFoundException("conta demo ainda não foi criada"));
        return issue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));
    }

    /** Vincula o numero do WhatsApp a conta. Aceita "(19) 99999-9999", "+55 19 ..." etc.; guarda so digitos com DDI. */
    @Transactional
    public UserResponse linkPhone(UUID userId, String rawPhone) {
        var phone = normalizePhone(rawPhone);
        userRepository.findByPhone(phone)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new BusinessException("esse número já está vinculado a outra conta");
                });
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));
        user.linkPhone(phone);
        log.info("WhatsApp vinculado: user={} phone={}", userId, dio.budgeting.security.Masking.phone(phone));
        return userMapper.toResponse(userRepository.save(user));
    }

    /** Remove tudo que nao e digito; numeros brasileiros sem DDI (10 ou 11 digitos) ganham o 55. */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            throw new BusinessException("informe o número do WhatsApp");
        }
        var digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }
        if (digits.length() < 12 || digits.length() > 15) {
            throw new BusinessException("número inválido: use o formato (DDD) 99999-9999 ou +55 DDD 99999-9999");
        }
        return digits;
    }

    private AuthResponse issue(User user) {
        return AuthResponse.bearer(jwtService.generate(user), jwtService.expiresInSeconds(), userMapper.toResponse(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
