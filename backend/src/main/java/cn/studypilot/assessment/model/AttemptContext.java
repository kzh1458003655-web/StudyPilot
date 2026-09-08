package cn.studypilot.assessment.model;
import java.util.List;
public record AttemptContext(long attemptId, long examId, List<AssessableItem> items) {}
