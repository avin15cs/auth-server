package io.github.avin15cs.authserver.admin;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAuditRepository {

    private final JdbcOperations jdbc;

    public AdminAuditRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID actorUserId, String action, String targetType, String targetId, String details) {
        jdbc.update("""
                INSERT INTO admin_audit_event (
                    id, actor_user_id, action, target_type, target_id, details
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), actorUserId, action, targetType, targetId, details);
    }
}
