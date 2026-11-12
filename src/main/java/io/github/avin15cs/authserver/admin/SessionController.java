package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.security.CurrentUserService;
import io.github.avin15cs.authserver.user.AuthUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final CurrentUserService currentUser;

    public SessionController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
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
