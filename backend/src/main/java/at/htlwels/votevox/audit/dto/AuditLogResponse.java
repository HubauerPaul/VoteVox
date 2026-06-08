package at.htlwels.votevox.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant timestamp,
        String userType,
        UUID userId,
        String actionType,
        String details
) {}
