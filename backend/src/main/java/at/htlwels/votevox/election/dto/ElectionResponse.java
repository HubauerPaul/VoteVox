package at.htlwels.votevox.election.dto;

import at.htlwels.votevox.election.ElectionStatus;
import at.htlwels.votevox.election.ElectionType;

import java.time.Instant;
import java.util.UUID;

public record ElectionResponse(
        UUID id,
        String title,
        String description,
        ElectionType type,
        Instant startTime,
        Instant endTime,
        ElectionStatus status,
        Instant createdAt,
        long candidateCount,
        long studentCount,
        long voteCount
) {}
