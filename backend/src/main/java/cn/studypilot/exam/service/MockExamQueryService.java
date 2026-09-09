package cn.studypilot.exam.service;

import cn.studypilot.common.exception.ResourceNotFoundException;
import cn.studypilot.exam.model.MockExamDetail;
import cn.studypilot.exam.repository.MockExamRepository;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;
import org.springframework.stereotype.Service;

/** Keeps generated-paper lookup project-scoped, matching the rest of the product API. */
@Service
@ConditionalOnStudyPilotDatabase
public class MockExamQueryService {
  private final MockExamRepository exams;
  public MockExamQueryService(MockExamRepository exams) { this.exams = exams; }
  public MockExamDetail find(long projectId, long examId) {
    return exams.findByProject(projectId, examId)
        .orElseThrow(() -> new ResourceNotFoundException("模拟卷不存在或不属于当前项目"));
  }
  public java.util.List<cn.studypilot.exam.model.MockExamSummary> list(long projectId) {
    return exams.listByProject(projectId);
  }
}
