package dio.budgeting.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Le o usuario autenticado da requisicao atual. Usa {@code getName()} (o subject do JWT),
 * que tambem funciona com {@code @WithMockUser(username = "<uuid>")} nos testes.
 */
@Component
public class CurrentUserProvider {

    public UUID requireUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("nenhum usuário autenticado");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("token sem identificador de usuário válido");
        }
    }
}
