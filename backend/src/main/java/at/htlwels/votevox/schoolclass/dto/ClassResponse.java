package at.htlwels.votevox.schoolclass.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassResponse(
        UUID id,
        String name,
        int studentCount,
        Instant createdAt
) {}
