package at.htlwels.votevox.voting.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank String token
) {}
