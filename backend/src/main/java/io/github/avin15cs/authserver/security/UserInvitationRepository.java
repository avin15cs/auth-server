package io.github.avin15cs.authserver.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserInvitationRepository {

    private static final RowMapper<UserInvitation> ROW_MAPPER = UserInvitationRepository::map;
    private final JdbcOperations jdbc;

    public UserInvitationRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public void revokePendingForEmail(String email) {
        jdbc.update("""
                UPDATE auth_user_invitation
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE lower(email) = lower(?)
                  AND consumed_at IS NULL AND revoked_at IS NULL
                """, email);
    }

    public void insert(UserInvitation invitation) {
        jdbc.update("""
                INSERT INTO auth_user_invitation (
                    id, email, token_hash, grant_auth_admin, expires_at, created_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, invitation.id(), invitation.email(), invitation.tokenHash(), invitation.grantAuthAdmin(),
                OffsetDateTime.ofInstant(invitation.expiresAt(), java.time.ZoneOffset.UTC),
                invitation.createdBy(),
                OffsetDateTime.ofInstant(invitation.createdAt(), java.time.ZoneOffset.UTC));
    }

    public Optional<UserInvitation> findUsableByTokenHash(String tokenHash) {
        return jdbc.query("""
                SELECT id, email, token_hash, grant_auth_admin, expires_at, created_by, created_at,
                       consumed_at, consumed_user_id, revoked_at
                FROM auth_user_invitation
                WHERE token_hash = ? AND consumed_at IS NULL AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """, ROW_MAPPER, tokenHash).stream().findFirst();
    }

    public List<UserInvitation> findRecent(int limit) {
        return jdbc.query("""
                SELECT id, email, token_hash, grant_auth_admin, expires_at, created_by, created_at,
                       consumed_at, consumed_user_id, revoked_at
                FROM auth_user_invitation
                ORDER BY created_at DESC
                LIMIT ?
                """, ROW_MAPPER, limit);
    }

    public boolean consume(UUID id, UUID userId) {
        return jdbc.update("""
                UPDATE auth_user_invitation
                SET consumed_at = CURRENT_TIMESTAMP, consumed_user_id = ?
                WHERE id = ? AND consumed_at IS NULL AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """, userId, id) == 1;
    }

    public boolean revoke(UUID id) {
        return jdbc.update("""
                UPDATE auth_user_invitation
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE id = ? AND consumed_at IS NULL AND revoked_at IS NULL
                """, id) == 1;
    }

    private static UserInvitation map(ResultSet rs, int rowNumber) throws SQLException {
        return new UserInvitation(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("token_hash"),
                rs.getBoolean("grant_auth_admin"),
                instant(rs, "expires_at"),
                rs.getObject("created_by", UUID.class),
                instant(rs, "created_at"),
                nullableInstant(rs, "consumed_at"),
                rs.getObject("consumed_user_id", UUID.class),
                nullableInstant(rs, "revoked_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    public record UserInvitation(
            UUID id,
            String email,
            String tokenHash,
            boolean grantAuthAdmin,
            Instant expiresAt,
            UUID createdBy,
            Instant createdAt,
            Instant consumedAt,
            UUID consumedUserId,
            Instant revokedAt) {
    }
}
