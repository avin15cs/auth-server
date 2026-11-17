package io.github.avin15cs.authserver.security;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LocalCredentialRepository {

    private final JdbcOperations jdbc;

    public LocalCredentialRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<LocalCredential> findByUsername(String username) {
        return jdbc.query("""
                SELECT user_id, username, password_hash, enabled, credential_source
                FROM auth_local_credential
                WHERE username = ?
                """, (rs, rowNumber) -> new LocalCredential(
                rs.getObject("user_id", UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getBoolean("enabled"),
                rs.getString("credential_source")), normalize(username)).stream().findFirst();
    }

    @Transactional
    public void upsertBootstrap(UUID userId, String username, String passwordHash) {
        String normalizedUsername = normalize(username);
        jdbc.update("""
                UPDATE auth_local_credential
                SET enabled = false, updated_at = CURRENT_TIMESTAMP
                WHERE credential_source = 'local-bootstrap' AND username <> ? AND enabled = true
                """, normalizedUsername);
        int upserted = jdbc.update("""
                INSERT INTO auth_local_credential (
                    user_id, username, password_hash, enabled, credential_source, created_at, updated_at
                ) VALUES (?, ?, ?, true, 'local-bootstrap', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (username) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    password_hash = EXCLUDED.password_hash,
                    enabled = true,
                    credential_source = 'local-bootstrap',
                    updated_at = CURRENT_TIMESTAMP
                WHERE auth_local_credential.credential_source = 'local-bootstrap'
                """, userId, normalizedUsername, passwordHash);
        if (upserted != 1) {
            throw new IllegalStateException("The local bootstrap username is already used by an invited account");
        }
    }

    public void createInvited(UUID userId, String username, String passwordHash) {
        jdbc.update("""
                INSERT INTO auth_local_credential (
                    user_id, username, password_hash, enabled, credential_source, created_at, updated_at
                ) VALUES (?, ?, ?, true, 'invitation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, normalize(username), passwordHash);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public record LocalCredential(
            UUID userId, String username, String passwordHash, boolean enabled, String credentialSource) {
    }
}
