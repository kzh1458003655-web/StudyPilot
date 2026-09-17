package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cn.studypilot.common.exception.ResourceNotFoundException;
import cn.studypilot.exam.repository.MockExamRepository;
import cn.studypilot.exam.service.MockExamQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MockExamQueryServiceTest {
  @Test void deletesOnlyWhenTheExamBelongsToTheProject() {
    MockExamRepository exams = Mockito.mock(MockExamRepository.class);
    given(exams.deleteByProject(7L, 31L)).willReturn(true);
    new MockExamQueryService(exams).delete(7L, 31L);
    verify(exams).deleteByProject(7L, 31L);
  }

  @Test void rejectsMissingOrCrossProjectExam() {
    MockExamRepository exams = Mockito.mock(MockExamRepository.class);
    given(exams.deleteByProject(7L, 31L)).willReturn(false);
    assertThatThrownBy(() -> new MockExamQueryService(exams).delete(7L, 31L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("模拟卷不存在或不属于当前项目");
  }
}
