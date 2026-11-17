package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalCredentialRepository credentials;
    private final AuthUserRoleRepository roles;
    private final PasswordLoginAvailability availability;

    public LocalUserDetailsService(
            LocalCredentialRepository credentials,
            AuthUserRoleRepository roles,
            PasswordLoginAvailability availability) {
        this.credentials = credentials;
        this.roles = roles;
        this.availability = availability;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!availability.enabled()) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
        LocalCredentialRepository.LocalCredential credential = credentials.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        boolean sourceEnabled = switch (credential.credentialSource()) {
            case "local-bootstrap" -> availability.localBootstrapEnabled();
            case "invitation" -> availability.invitationsEnabled();
            default -> false;
        };
        if (!sourceEnabled) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
        List<SimpleGrantedAuthority> authorities = roles
                .findRoles(credential.userId(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new LocalUserPrincipal(
                credential.userId(), credential.username(), credential.passwordHash(),
                credential.enabled(), List.copyOf(authorities));
    }
}
