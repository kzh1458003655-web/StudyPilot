package cn.studypilot.qa.model;

import java.util.List;

/** One persisted user question and its completed assistant answer. */
public record QaHistoryTurn(String question, String answer, List<QaHistoryCitation> citations) {}
