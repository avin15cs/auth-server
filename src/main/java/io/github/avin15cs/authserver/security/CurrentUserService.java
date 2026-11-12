package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final AuthUserRepository users;
    private final AuthUserRoleRepository roles;

    public CurrentUserService(AuthUserRepository users, AuthUserRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    public AuthUser require(OidcUser principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByProviderSubject("google", principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public boolean isAdmin(AuthUser user) {
        return roles.findRoles(user.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE)
                .contains(AuthUserRoleRepository.AUTH_ADMIN_ROLE);
    }
}
