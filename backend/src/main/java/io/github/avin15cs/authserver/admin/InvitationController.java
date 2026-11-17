package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.security.UserInvitationService;
import io.github.avin15cs.authserver.security.UserInvitationService.InvitationSetup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final UserInvitationService invitations;

    public InvitationController(UserInvitationService invitations) {
        this.invitations = invitations;
    }

    @GetMapping("/setup")
    public InvitationSetupView get(
            @RequestHeader("X-Invitation-Token") @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            CsrfToken csrfToken) {
        InvitationSetup setup = invitations.getSetup(token);
        return new InvitationSetupView(setup.email(), setup.expiresAt(),
                new CsrfView(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(
            @RequestHeader("X-Invitation-Token") @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        invitations.accept(token, request.username(), request.displayName(), request.password());
    }

    public record AcceptInvitationRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,64}") String username,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(min = 12, max = 128) String password) {
    }

    public record InvitationSetupView(
            String email, java.time.Instant expiresAt, CsrfView csrf) {
    }

    public record CsrfView(String headerName, String token) {
    }
}
