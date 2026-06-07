package at.htlwels.votevox.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportStudentsRequest(
        @NotEmpty @Valid List<StudentRequest> students
) {}
