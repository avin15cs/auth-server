package io.github.avin15cs.authserver.user;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthUserRoleRepository {

    public static final String AUTH_SERVER_AUDIENCE = "auth-server";
    public static final String AUTH_ADMIN_ROLE = "AUTH_ADMIN";

    private final JdbcOperations jdbc;

    public AuthUserRoleRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> findRoles(UUID userId, String audience) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT role
                FROM auth_user_role
                WHERE user_id = ? AND audience = ?
                ORDER BY role
                """, String.class, userId, audience));
    }

    public Map<String, Set<String>> findRoles(UUID userId) {
        Map<String, Set<String>> roles = new LinkedHashMap<>();
        jdbc.query("""
                SELECT audience, role
                FROM auth_user_role
                WHERE user_id = ?
                ORDER BY audience, role
                """, (rs, rowNumber) -> Map.entry(rs.getString("audience"), rs.getString("role")), userId)
                .forEach(entry -> roles.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashSet<>())
                        .add(entry.getValue()));
        return roles;
    }

    @Transactional
    public boolean grantBootstrapAdministrator(AuthUser user, List<String> bootstrapEmails) {
        if (!user.emailVerified() || user.email() == null) {
            return false;
        }
        boolean configured = bootstrapEmails.stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .anyMatch(user.email().trim().toLowerCase(Locale.ROOT)::equals);
        if (!configured) {
            return false;
        }

        int inserted = jdbc.update("""
                INSERT INTO auth_user_role (user_id, audience, role, assignment_source)
                VALUES (?, ?, ?, 'bootstrap')
                ON CONFLICT (user_id, audience, role) DO NOTHING
                """, user.id(), AUTH_SERVER_AUDIENCE, AUTH_ADMIN_ROLE);
        if (inserted == 1) {
            insertAuditEvent(null, "BOOTSTRAP_ADMIN_GRANTED", "AUTH_USER", user.id().toString(),
                    "audience=auth-server,role=AUTH_ADMIN");
        }
        return inserted == 1;
    }

    @Transactional
    public void replaceRoles(UUID targetUserId, String audience, Set<String> roles, UUID actorUserId) {
        jdbc.update("DELETE FROM auth_user_role WHERE user_id = ? AND audience = ?", targetUserId, audience);
        roles.forEach(role -> jdbc.update("""
                INSERT INTO auth_user_role (
                    user_id, audience, role, assignment_source, assigned_by
                ) VALUES (?, ?, ?, 'admin', ?)
                """, targetUserId, audience, role, actorUserId));
        insertAuditEvent(actorUserId, "USER_ROLES_REPLACED", "AUTH_USER", targetUserId.toString(),
                "audience=" + audience + ",roles=" + String.join(",", roles));
    }

    private void insertAuditEvent(
            UUID actorUserId, String action, String targetType, String targetId, String details) {
        jdbc.update("""
                INSERT INTO admin_audit_event (
                    id, actor_user_id, action, target_type, target_id, details
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), actorUserId, action, targetType, targetId, details);
    }
}
