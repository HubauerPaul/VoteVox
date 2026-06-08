package at.htlwels.votevox.schoolclass.dto;

import java.util.UUID;

/**
 * A class shown in the election's class picker: its details plus whether it is
 * currently selected for that election.
 */
public record ElectionClassOption(
        UUID classId,
        String name,
        int studentCount,
        boolean selected
) {}
