package cn.studypilot.assessment.repository;
import cn.studypilot.assessment.model.*;
import java.util.List;
public interface AssessmentRepository { long createAttempt(long projectId, long examId); AttemptContext findInProgressAttempt(long projectId, long attemptId); void saveGradedAnswer(long attemptId, GradedAnswer graded, String answer); void completeAttempt(long attemptId, double totalScore); void updateMastery(long projectId, String point, double mastery); void recordWrongAnswer(long projectId, long attemptId, GradedAnswer graded); void recordRecommendation(long projectId, long attemptId, String point, String recommendation); }
