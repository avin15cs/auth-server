package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.security.CurrentUserService;
import io.github.avin15cs.authserver.security.UserInvitationService;
import io.github.avin15cs.authserver.security.UserInvitationService.InvitationCreation;
import io.github.avin15cs.authserver.security.UserInvitationService.InvitationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/invitations")
@Tag(name = "User invitations", description = "Audited username/password invitation lifecycle")
public class AdminInvitationController {

    private final UserInvitationService invitations;
    private final CurrentUserService currentUser;

    public AdminInvitationController(UserInvitationService invitations, CurrentUserService currentUser) {
        this.invitations = invitations;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List recent user invitations")
    public List<InvitationView> list() {
        return invitations.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user invitation", description = "Returns the setup URL only once.")
    public InvitationCreation create(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal Object principal) {
        return invitations.create(request.email(), request.grantAuthAdmin(), currentUser.require(principal).id());
    }

    @DeleteMapping("/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a pending user invitation")
    public void revoke(@PathVariable UUID invitationId, @AuthenticationPrincipal Object principal) {
        invitations.revoke(invitationId, currentUser.require(principal).id());
    }

    public record CreateInvitationRequest(
            @NotBlank @Email @Size(max = 254) String email,
            boolean grantAuthAdmin) {
    }
}
