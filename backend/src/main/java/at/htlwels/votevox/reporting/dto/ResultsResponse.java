package at.htlwels.votevox.reporting.dto;

import at.htlwels.votevox.election.ElectionStatus;
import at.htlwels.votevox.election.ElectionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResultsResponse(
        UUID electionId,
        String electionTitle,
        ElectionType electionType,
        ElectionStatus status,
        Instant generatedAt,
        List<CandidateResult> candidates,
        long totalVotes,
        List<BreakdownEntry> byClass,
        List<BreakdownEntry> byDepartment
) {}
