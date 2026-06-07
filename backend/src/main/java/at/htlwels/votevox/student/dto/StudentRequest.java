package at.htlwels.votevox.student.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentRequest(
        @NotBlank String name,
        @NotBlank String studentId,
        @NotBlank String className,
        @NotBlank String department
) {}
