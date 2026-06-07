package at.htlwels.votevox.voting.dto;

import at.htlwels.votevox.election.ElectionType;

import java.util.List;
import java.util.UUID;

public record ValidateTokenResponse(
        UUID electionId,
        String electionTitle,
        ElectionType electionType,
        List<BallotCandidate> candidates
) {}
