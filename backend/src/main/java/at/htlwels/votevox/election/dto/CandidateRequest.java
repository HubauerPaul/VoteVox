package at.htlwels.votevox.election.dto;

import jakarta.validation.constraints.NotBlank;

public record CandidateRequest(
        @NotBlank String name,
        @NotBlank String className,
        @NotBlank String department
) {}
