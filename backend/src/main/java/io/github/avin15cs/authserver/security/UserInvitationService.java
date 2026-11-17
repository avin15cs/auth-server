package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.admin.AdminAuditRepository;
import io.github.avin15cs.authserver.config.PasswordLoginProperties;
import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserInvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserInvitationRepository invitations;
    private final AuthUserRepository users;
    private final LocalCredentialRepository credentials;
    private final AuthUserRoleRepository roles;
    private final AdminAuditRepository audit;
    private final PasswordEncoder passwordEncoder;
    private final PasswordLoginProperties properties;
    private final PasswordLoginAvailability availability;
    private final String uiOrigin;

    public UserInvitationService(
            UserInvitationRepository invitations,
            AuthUserRepository users,
            LocalCredentialRepository credentials,
            AuthUserRoleRepository roles,
            AdminAuditRepository audit,
            PasswordEncoder passwordEncoder,
            PasswordLoginProperties properties,
            PasswordLoginAvailability availability,
            @Value("${auth.ui-origin:http://localhost:5173/}") String uiOrigin) {
        this.invitations = invitations;
        this.users = users;
        this.credentials = credentials;
        this.roles = roles;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.availability = availability;
        this.uiOrigin = uiOrigin;
    }

    @Transactional
    public InvitationCreation create(String email, boolean grantAuthAdmin, UUID actorUserId) {
        requireInvitationsEnabled();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.localUserExistsForEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A password account already exists for this email");
        }
        String token = generateToken();
        Instant now = Instant.now();
        UserInvitationRepository.UserInvitation invitation = new UserInvitationRepository.UserInvitation(
                UUID.randomUUID(), normalizedEmail, hash(token), grantAuthAdmin,
                now.plus(properties.invitationTtl()), actorUserId, now, null, null, null);
        invitations.revokePendingForEmail(normalizedEmail);
        invitations.insert(invitation);
        audit.record(actorUserId, "USER_INVITATION_CREATED", "USER_INVITATION",
                invitation.id().toString(), "email=" + normalizedEmail + ",authAdmin=" + grantAuthAdmin);
        return new InvitationCreation(toView(invitation), setupUrl(token));
    }

    public List<InvitationView> list() {
        requireInvitationsEnabled();
        return invitations.findRecent(100).stream().map(this::toView).toList();
    }

    public InvitationSetup getSetup(String token) {
        requireInvitationsEnabled();
        UserInvitationRepository.UserInvitation invitation = requireUsable(token);
        return new InvitationSetup(invitation.email(), invitation.expiresAt());
    }

    @Transactional
    public void accept(String token, String username, String displayName, String password) {
        requireInvitationsEnabled();
        UserInvitationRepository.UserInvitation invitation = requireUsable(token);
        AuthUser user;
        try {
            user = users.createInvitedLocalUser(username, invitation.email(), displayName.trim());
            credentials.createInvited(user.id(), username, passwordEncoder.encode(password));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }
        if (!invitations.consume(invitation.id(), user.id())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation is no longer valid");
        }
        if (invitation.grantAuthAdmin()) {
            roles.grantInvitedAdministrator(user, invitation.createdBy());
        }
        audit.record(invitation.createdBy(), "USER_INVITATION_ACCEPTED", "AUTH_USER",
                user.id().toString(), "invitationId=" + invitation.id());
    }

    @Transactional
    public void revoke(UUID invitationId, UUID actorUserId) {
        requireInvitationsEnabled();
        if (!invitations.revoke(invitationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation is no longer pending");
        }
        audit.record(actorUserId, "USER_INVITATION_REVOKED", "USER_INVITATION",
                invitationId.toString(), "revoked=true");
    }

    private UserInvitationRepository.UserInvitation requireUsable(String token) {
        return invitations.findUsableByTokenHash(hash(token))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "This invitation is invalid, expired, or already used"));
    }

    private void requireInvitationsEnabled() {
        if (!availability.invitationsEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private InvitationView toView(UserInvitationRepository.UserInvitation invitation) {
        String status;
        if (invitation.consumedAt() != null) {
            status = "ACCEPTED";
        } else if (invitation.revokedAt() != null) {
            status = "REVOKED";
        } else if (!invitation.expiresAt().isAfter(Instant.now())) {
            status = "EXPIRED";
        } else {
            status = "PENDING";
        }
        return new InvitationView(invitation.id(), invitation.email(), invitation.grantAuthAdmin(),
                invitation.expiresAt(), invitation.createdAt(), status);
    }

    private String setupUrl(String token) {
        return uiOrigin.replaceAll("/+$", "") + "/invite#token=" + token;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record InvitationView(
            UUID id, String email, boolean grantAuthAdmin, Instant expiresAt, Instant createdAt, String status) {
    }

    public record InvitationCreation(InvitationView invitation, String setupUrl) {
    }

    public record InvitationSetup(String email, Instant expiresAt) {
    }
}
