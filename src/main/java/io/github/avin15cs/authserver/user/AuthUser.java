package io.github.avin15cs.authserver.user;

import java.time.Instant;
import java.util.UUID;

public record AuthUser(
        UUID id,
        String provider,
        String providerSubject,
        String email,
        boolean emailVerified,
        String displayName,
        String pictureUrl,
        Instant createdAt,
        Instant lastLoginAt) {
}

