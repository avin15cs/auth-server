package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.user.AuthUserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;

@Service
public class PersistingOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final AuthUserRepository users;

    public PersistingOidcUserService(AuthUserRepository users) {
        this.users = users;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser user = delegate.loadUser(request);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"),
                    "Google account email must be verified");
        }
        users.upsertGoogleUser(
                user.getSubject(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getFullName(),
                user.getPicture());
        return user;
    }
}
