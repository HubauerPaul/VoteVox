package at.htlwels.votevox.schoolclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload for a global class. */
public record ClassRequest(
        @NotBlank @Size(max = 64) String name,
        @Min(0) int studentCount
) {}
