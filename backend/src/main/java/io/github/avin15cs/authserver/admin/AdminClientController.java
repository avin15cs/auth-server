package io.github.avin15cs.authserver.admin;

import io.github.avin15cs.authserver.client.ClientManagementService;
import io.github.avin15cs.authserver.client.ClientManagementService.ClientCreation;
import io.github.avin15cs.authserver.client.ClientManagementService.ClientPage;
import io.github.avin15cs.authserver.client.ClientManagementService.ClientView;
import io.github.avin15cs.authserver.client.ClientType;
import io.github.avin15cs.authserver.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/clients")
public class AdminClientController {

    private final ClientManagementService clients;
    private final CurrentUserService currentUser;

    public AdminClientController(ClientManagementService clients, CurrentUserService currentUser) {
        this.clients = clients;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ClientPage list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return clients.list(search, limit, offset);
    }

    @GetMapping("/{id}")
    public ClientView get(@PathVariable String id) {
        return clients.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientCreation create(
            @Valid @RequestBody CreateClientRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return clients.create(request.toCommand(), currentUser.require(principal).id());
    }

    @PutMapping("/{id}")
    public ClientView update(
            @PathVariable String id,
            @Valid @RequestBody UpdateClientRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return clients.update(id, request.toCommand(), currentUser.require(principal).id());
    }

    @PutMapping("/{id}/enabled")
    public ClientView setEnabled(
            @PathVariable String id,
            @Valid @RequestBody EnabledRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return clients.setEnabled(id, request.enabled(), currentUser.require(principal).id());
    }

    public record CreateClientRequest(
            String clientId,
            String name,
            ClientType clientType,
            boolean publicClient,
            Set<String> grantTypes,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String audience) {
        ClientManagementService.CreateCommand toCommand() {
            return new ClientManagementService.CreateCommand(
                    clientId, name, clientType, publicClient, grantTypes, redirectUris,
                    postLogoutRedirectUris, scopes, audience);
        }
    }

    public record UpdateClientRequest(
            String name,
            Set<String> grantTypes,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String audience) {
        ClientManagementService.UpdateCommand toCommand() {
            return new ClientManagementService.UpdateCommand(
                    name, grantTypes, redirectUris, postLogoutRedirectUris, scopes, audience);
        }
    }

    public record EnabledRequest(boolean enabled) {
    }
}
