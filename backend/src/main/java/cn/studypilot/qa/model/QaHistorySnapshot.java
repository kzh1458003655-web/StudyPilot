package cn.studypilot.qa.model;

import java.util.List;

/** Most recently active conversation for a course. */
public record QaHistorySnapshot(Long sessionId, List<QaHistoryTurn> turns) {
  public static QaHistorySnapshot empty() { return new QaHistorySnapshot(null, List.of()); }
}
