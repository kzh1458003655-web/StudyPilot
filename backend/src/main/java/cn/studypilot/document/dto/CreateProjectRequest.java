package cn.studypilot.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Project is the required data boundary for document and QA routes. */
public record CreateProjectRequest(@NotBlank @Size(max = 100) String name, @Size(max = 1000) String description) {}
