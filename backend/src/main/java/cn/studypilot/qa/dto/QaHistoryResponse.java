package cn.studypilot.qa.dto;

import java.util.List;

public record QaHistoryResponse(Long sessionId, List<QaHistoryTurnResponse> turns) {
  public record QaHistoryTurnResponse(String question, String answer, List<QaCitationResponse> citations) {}
}
