package at.htlwels.votevox.schoolclass.dto;

import java.util.List;
import java.util.UUID;

/** Replaces the set of classes participating in an election. */
public record SetElectionClassesRequest(
        List<UUID> classIds
) {}
