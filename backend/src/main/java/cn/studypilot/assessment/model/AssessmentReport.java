package cn.studypilot.assessment.model;
import java.util.List;
public record AssessmentReport(long attemptId, double totalScore, double maxScore, List<GradedAnswer> answers) {}
