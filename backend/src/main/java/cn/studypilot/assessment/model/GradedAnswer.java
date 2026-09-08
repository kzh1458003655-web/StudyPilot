package cn.studypilot.assessment.model;
public record GradedAnswer(long itemId, String knowledgePoint, double score, double maxScore, String feedback, String gradingStatus) {}
