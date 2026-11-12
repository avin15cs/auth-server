package io.github.avin15cs.authserver.client;

import java.time.Instant;
import java.util.UUID;

public record ClientMetadata(
        String registeredClientId,
        ClientType clientType,
        String audience,
        boolean enabled,
        String source,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt) {
}
