package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.config.LocalLoginProperties;
import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalAdministratorBootstrap implements ApplicationRunner {

    private final LocalLoginProperties properties;
    private final AuthUserRepository users;
    private final LocalCredentialRepository credentials;
    private final AuthUserRoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public LocalAdministratorBootstrap(
            LocalLoginProperties properties,
            AuthUserRepository users,
            LocalCredentialRepository credentials,
            AuthUserRoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.credentials = credentials;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        if (properties.username().isBlank() || properties.password().isBlank()) {
            throw new IllegalStateException(
                    "LOCAL_ADMIN_USERNAME and LOCAL_ADMIN_PASSWORD are required when local login is enabled");
        }
        AuthUser user = users.upsertLocalUser(properties.username(), "Local Administrator");
        credentials.upsertBootstrap(
                user.id(), properties.username(), passwordEncoder.encode(properties.password()));
        roles.grantLocalBootstrapAdministrator(user);
    }
}
