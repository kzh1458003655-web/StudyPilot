package cn.studypilot.exam.repository;

import cn.studypilot.exam.model.GeneratedExamItem;
import cn.studypilot.exam.model.MockExamDetail;
import cn.studypilot.exam.model.SavedMockExam;
import java.util.List;
import java.util.Optional;
public interface MockExamRepository {
  SavedMockExam save(long projectId, String title, List<GeneratedExamItem> items);
  Optional<MockExamDetail> findByProject(long projectId, long examId);
}
