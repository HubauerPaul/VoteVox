package at.htlwels.votevox.reporting.dto;

/**
 * A breakdown entry for total participation, grouped by class or department.
 */
public record BreakdownEntry(
        String label,
        long eligible,
        long tokensIssued
) {}
