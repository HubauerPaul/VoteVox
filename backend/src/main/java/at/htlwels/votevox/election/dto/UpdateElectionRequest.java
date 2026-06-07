package at.htlwels.votevox.election.dto;

import at.htlwels.votevox.election.ElectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateElectionRequest(
        @NotBlank String title,
        String description,
        @NotNull ElectionType type,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {}
