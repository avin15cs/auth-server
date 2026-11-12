package io.github.avin15cs.authserver.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthUserRepository {

    private static final RowMapper<AuthUser> ROW_MAPPER = AuthUserRepository::mapUser;
    private final JdbcOperations jdbc;

    public AuthUserRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public AuthUser upsertGoogleUser(
            String subject,
            String email,
            boolean emailVerified,
            String displayName,
            String pictureUrl) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO auth_user (
                    id, provider, provider_subject, email, email_verified,
                    display_name, picture_url, created_at, last_login_at
                ) VALUES (?, 'google', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (provider, provider_subject) DO UPDATE SET
                    email = EXCLUDED.email,
                    email_verified = EXCLUDED.email_verified,
                    display_name = EXCLUDED.display_name,
                    picture_url = EXCLUDED.picture_url,
                    last_login_at = CURRENT_TIMESTAMP
                """, id, subject, email, emailVerified, displayName, pictureUrl);

        return findByProviderSubject("google", subject)
                .orElseThrow(() -> new IllegalStateException("User was not persisted"));
    }

    public Optional<AuthUser> findByProviderSubject(String provider, String subject) {
        return jdbc.query("""
                SELECT id, provider, provider_subject, email, email_verified,
                       display_name, picture_url, created_at, last_login_at
                FROM auth_user
                WHERE provider = ? AND provider_subject = ?
                """, ROW_MAPPER, provider, subject).stream().findFirst();
    }

    public Optional<AuthUser> findById(UUID id) {
        return jdbc.query("""
                SELECT id, provider, provider_subject, email, email_verified,
                       display_name, picture_url, created_at, last_login_at
                FROM auth_user
                WHERE id = ?
                """, ROW_MAPPER, id).stream().findFirst();
    }

    public long count() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM auth_user", Long.class);
        return count == null ? 0 : count;
    }

    public List<AuthUser> findPage(int limit, int offset) {
        return jdbc.query("""
                SELECT id, provider, provider_subject, email, email_verified,
                       display_name, picture_url, created_at, last_login_at
                FROM auth_user
                ORDER BY created_at DESC, id
                LIMIT ? OFFSET ?
                """, ROW_MAPPER, limit, offset);
    }

    private static AuthUser mapUser(ResultSet rs, int rowNumber) throws SQLException {
        return new AuthUser(
                rs.getObject("id", UUID.class),
                rs.getString("provider"),
                rs.getString("provider_subject"),
                rs.getString("email"),
                rs.getBoolean("email_verified"),
                rs.getString("display_name"),
                rs.getString("picture_url"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("last_login_at", OffsetDateTime.class).toInstant());
    }
}
