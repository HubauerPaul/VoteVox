package at.htlwels.votevox.auth.dto;

import at.htlwels.votevox.auth.AdminRole;

public record LoginResponse(
        String token,
        AdminRole role,
        String name
) {}
