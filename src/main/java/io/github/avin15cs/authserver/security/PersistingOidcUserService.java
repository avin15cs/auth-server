package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.config.AuthServerProperties;
import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class PersistingOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final AuthUserRepository users;
    private final AuthUserRoleRepository roles;
    private final AuthServerProperties properties;

    public PersistingOidcUserService(
            AuthUserRepository users,
            AuthUserRoleRepository roles,
            AuthServerProperties properties) {
        this.users = users;
        this.roles = roles;
        this.properties = properties;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser user = delegate.loadUser(request);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"),
                    "Google account email must be verified");
        }
        AuthUser persisted = users.upsertGoogleUser(
                user.getSubject(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getFullName(),
                user.getPicture());
        roles.grantBootstrapAdministrator(persisted, properties.admin().bootstrapEmails());

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
        roles.findRoles(persisted.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        if (user.getUserInfo() == null) {
            return new DefaultOidcUser(authorities, user.getIdToken(), IdTokenClaimNames.SUB);
        }
        return new DefaultOidcUser(
                authorities, user.getIdToken(), user.getUserInfo(), IdTokenClaimNames.SUB);
    }
}
