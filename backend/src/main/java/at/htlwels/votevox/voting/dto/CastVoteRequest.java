package at.htlwels.votevox.voting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CastVoteRequest(
        @NotBlank String token,
        @NotNull UUID candidateId
) {}
