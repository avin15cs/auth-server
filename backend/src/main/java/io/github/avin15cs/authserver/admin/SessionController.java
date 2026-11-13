package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.security.CurrentUserService;
import io.github.avin15cs.authserver.user.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@Tag(name = "Session", description = "Current administrator session and CSRF contract")
public class SessionController {

    private final CurrentUserService currentUser;

    public SessionController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Get the current session", description = "Returns safe user fields, the persisted "
            + "AUTH_ADMIN decision, and the CSRF header and token required for mutations.")
    public SessionView session(@AuthenticationPrincipal OidcUser principal, CsrfToken csrfToken) {
        AuthUser user = currentUser.require(principal);
        return new SessionView(
                new UserView(user.id(), user.email(), user.displayName(), user.pictureUrl()),
                currentUser.isAdmin(user),
                new CsrfView(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    public record SessionView(UserView user, boolean admin, CsrfView csrf) {
    }

    public record UserView(UUID id, String email, String displayName, String pictureUrl) {
    }

    public record CsrfView(String headerName, String token) {
    }
}
