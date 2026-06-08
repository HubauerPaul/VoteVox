package at.htlwels.votevox.election.dto;

import java.util.UUID;

public record CandidateResponse(
        UUID electionCandidateId,
        UUID candidateId,
        String name,
        String className,
        String department
) {}
