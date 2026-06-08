package at.htlwels.votevox.voting.dto;

import java.util.UUID;

/**
 * Anonymized candidate option presented to the voter.
 * The {@code id} is the {@code ElectionCandidate} UUID, NOT the global Candidate id.
 */
public record BallotCandidate(
        UUID id,
        String name,
        String className,
        String department
) {}
