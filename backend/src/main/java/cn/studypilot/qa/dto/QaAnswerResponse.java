package cn.studypilot.qa.dto;

import java.util.List;

public record QaAnswerResponse(long sessionId, long userMessageId, long assistantMessageId, String status,
                               String answer, List<QaCitationResponse> citations) {}
