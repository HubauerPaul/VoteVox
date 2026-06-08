package at.htlwels.votevox.reporting.dto;

import java.util.UUID;

public record CandidateResult(
        UUID candidateId,
        UUID electionCandidateId,
        String name,
        String className,
        String department,
        long votes,
        double percentage
) {}
