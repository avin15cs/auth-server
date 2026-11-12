package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class AuthAdminAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthUserRepository users;
    private final AuthUserRoleRepository roles;

    public AuthAdminAuthorizationManager(
            AuthUserRepository users,
            AuthUserRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated() || !(current.getPrincipal() instanceof OidcUser principal)) {
            return new AuthorizationDecision(false);
        }
        boolean granted = users.findByProviderSubject("google", principal.getSubject())
                .map(user -> roles.findRoles(user.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE)
                        .contains(AuthUserRoleRepository.AUTH_ADMIN_ROLE))
                .orElse(false);
        return new AuthorizationDecision(granted);
    }
}
