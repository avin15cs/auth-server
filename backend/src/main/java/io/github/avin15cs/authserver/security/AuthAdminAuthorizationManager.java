package io.github.avin15cs.authserver.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class AuthAdminAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final CurrentUserService currentUser;

    public AuthAdminAuthorizationManager(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        boolean granted = currentUser.find(current.getPrincipal())
                .map(currentUser::isAdmin)
                .orElse(false);
        return new AuthorizationDecision(granted);
    }
}
