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
                SELECT user_id, username, password_hash, enabled
                FROM auth_local_credential
                WHERE username = ?
                """, (rs, rowNumber) -> new LocalCredential(
                rs.getObject("user_id", UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getBoolean("enabled")), normalize(username)).stream().findFirst();
    }

    @Transactional
    public void upsert(UUID userId, String username, String passwordHash) {
        String normalizedUsername = normalize(username);
        jdbc.update("""
                UPDATE auth_local_credential
                SET enabled = false, updated_at = CURRENT_TIMESTAMP
                WHERE username <> ? AND enabled = true
                """, normalizedUsername);
        jdbc.update("""
                INSERT INTO auth_local_credential (
                    user_id, username, password_hash, enabled, created_at, updated_at
                ) VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (username) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    password_hash = EXCLUDED.password_hash,
                    enabled = true,
                    updated_at = CURRENT_TIMESTAMP
                """, userId, normalizedUsername, passwordHash);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public record LocalCredential(UUID userId, String username, String passwordHash, boolean enabled) {
    }
}
