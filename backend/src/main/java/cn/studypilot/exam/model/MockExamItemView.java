package cn.studypilot.exam.model;

import java.util.List;

/** Read model for one persisted mock-exam item. */
public record MockExamItemView(long id, int ordinal, String type, String prompt, List<String> options,
                               String answer, String analysis, String knowledgePoint, double score) {
}
