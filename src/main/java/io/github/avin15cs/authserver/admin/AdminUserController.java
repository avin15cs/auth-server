package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;
    private final AuthUserRepository users;
    private final AuthUserRoleRepository roles;

    public AdminUserController(AuthUserRepository users, AuthUserRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @GetMapping
    public UserPage listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > MAX_PAGE_SIZE || offset < 0 || search.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        List<UserView> content = users.findPage(search, limit, offset).stream().map(this::toView).toList();
        return new UserPage(content, users.count(search), limit, offset);
    }

    @PutMapping("/{userId}/roles/{audience}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void replaceRoles(
            @PathVariable UUID userId,
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._:-]{1,200}") String audience,
            @Valid @RequestBody ReplaceRolesRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        AuthUser actor = currentUser(principal);
        AuthUser target = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (actor.id().equals(target.id())
                && AuthUserRoleRepository.AUTH_SERVER_AUDIENCE.equals(audience)
                && !request.roles().contains(AuthUserRoleRepository.AUTH_ADMIN_ROLE)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Administrators cannot remove their own AUTH_ADMIN role");
        }
        roles.replaceRoles(target.id(), audience, request.roles(), actor.id());
    }

    private AuthUser currentUser(OidcUser principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByProviderSubject("google", principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private UserView toView(AuthUser user) {
        return new UserView(
                user.id(), user.email(), user.emailVerified(), user.displayName(), user.pictureUrl(),
                user.createdAt(), user.lastLoginAt(), roles.findRoles(user.id()));
    }

    public record UserPage(List<UserView> content, long total, int limit, int offset) {
    }

    public record UserView(
            UUID id,
            String email,
            boolean emailVerified,
            String displayName,
            String pictureUrl,
            Instant createdAt,
            Instant lastLoginAt,
            Map<String, Set<String>> roles) {
    }

    public record ReplaceRolesRequest(
            @Size(max = 20)
            Set<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,99}") String> roles) {
        public ReplaceRolesRequest {
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }
    }
}
