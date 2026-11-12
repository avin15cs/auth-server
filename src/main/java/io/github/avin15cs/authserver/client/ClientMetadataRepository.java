package io.github.avin15cs.authserver.client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ClientMetadataRepository {

    private final JdbcOperations jdbc;

    public ClientMetadataRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ClientMetadata> findByRegisteredClientId(String registeredClientId) {
        return jdbc.query("""
                SELECT registered_client_id, client_type, audience, enabled, source,
                       created_by, created_at, updated_by, updated_at
                FROM auth_client_metadata
                WHERE registered_client_id = ?
                """, ClientMetadataRepository::map, registeredClientId).stream().findFirst();
    }

    public Optional<ClientMetadata> findByClientId(String clientId) {
        return jdbc.query("""
                SELECT metadata.registered_client_id, metadata.client_type, metadata.audience,
                       metadata.enabled, metadata.source, metadata.created_by, metadata.created_at,
                       metadata.updated_by, metadata.updated_at
                FROM auth_client_metadata metadata
                JOIN oauth2_registered_client client ON client.id = metadata.registered_client_id
                WHERE client.client_id = ?
                """, ClientMetadataRepository::map, clientId).stream().findFirst();
    }

    public List<String> findPage(String search, int limit, int offset) {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        return jdbc.queryForList("""
                SELECT client.id
                FROM oauth2_registered_client client
                JOIN auth_client_metadata metadata ON metadata.registered_client_id = client.id
                WHERE lower(client.client_id) LIKE ? OR lower(client.client_name) LIKE ?
                   OR lower(metadata.audience) LIKE ?
                ORDER BY client.client_id
                LIMIT ? OFFSET ?
                """, String.class, pattern, pattern, pattern, limit, offset);
    }

    public long count(String search) {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM oauth2_registered_client client
                JOIN auth_client_metadata metadata ON metadata.registered_client_id = client.id
                WHERE lower(client.client_id) LIKE ? OR lower(client.client_name) LIKE ?
                   OR lower(metadata.audience) LIKE ?
                """, Long.class, pattern, pattern, pattern);
        return count == null ? 0 : count;
    }

    public void insertDynamic(
            String registeredClientId, ClientType clientType, String audience, UUID actorUserId) {
        jdbc.update("""
                INSERT INTO auth_client_metadata (
                    registered_client_id, client_type, audience, enabled, source,
                    created_by, updated_by
                ) VALUES (?, ?, ?, true, 'DYNAMIC', ?, ?)
                """, registeredClientId, clientType.name(), audience, actorUserId, actorUserId);
    }

    public void upsertBootstrap(
            String registeredClientId, ClientType clientType, String audience, boolean enabled) {
        jdbc.update("""
                INSERT INTO auth_client_metadata (
                    registered_client_id, client_type, audience, enabled, source
                ) VALUES (?, ?, ?, ?, 'BOOTSTRAP')
                ON CONFLICT (registered_client_id) DO UPDATE SET
                    client_type = EXCLUDED.client_type,
                    audience = EXCLUDED.audience,
                    enabled = EXCLUDED.enabled,
                    updated_at = CURRENT_TIMESTAMP
                WHERE auth_client_metadata.source = 'BOOTSTRAP'
                """, registeredClientId, clientType.name(), audience, enabled);
    }

    public void update(String registeredClientId, String audience, UUID actorUserId) {
        jdbc.update("""
                UPDATE auth_client_metadata
                SET audience = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE registered_client_id = ?
                """, audience, actorUserId, registeredClientId);
    }

    public boolean setEnabled(String registeredClientId, boolean enabled, UUID actorUserId) {
        return jdbc.update("""
                UPDATE auth_client_metadata
                SET enabled = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE registered_client_id = ? AND enabled <> ?
                """, enabled, actorUserId, registeredClientId, enabled) == 1;
    }

    private static ClientMetadata map(ResultSet rs, int rowNumber) throws SQLException {
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
        return new ClientMetadata(
                rs.getString("registered_client_id"),
                ClientType.valueOf(rs.getString("client_type")),
                rs.getString("audience"),
                rs.getBoolean("enabled"),
                rs.getString("source"),
                rs.getObject("created_by", UUID.class),
                createdAt.toInstant(),
                rs.getObject("updated_by", UUID.class),
                updatedAt.toInstant());
    }
}
