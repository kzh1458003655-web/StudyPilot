package cn.studypilot.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Asking without a session starts a new, project-scoped conversation. */
public record AskQaRequest(
    @NotNull @Positive Long projectId,
    @Positive Long sessionId,
    @NotBlank @Size(max = 1000) String question) {}
