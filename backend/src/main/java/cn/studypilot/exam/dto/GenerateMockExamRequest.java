package cn.studypilot.exam.dto;

import jakarta.validation.constraints.Size;

/** Optional natural-language constraints supplied when creating a mock exam. */
public record GenerateMockExamRequest(@Size(max = 1000) String instructions) {
  public String normalizedInstructions() {
    return instructions == null ? "" : instructions.trim();
  }
}
