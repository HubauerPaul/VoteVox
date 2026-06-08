package at.htlwels.votevox.student.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String studentId,
        String className,
        String department,
        Instant createdAt
) {}
