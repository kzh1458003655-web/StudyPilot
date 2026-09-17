package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cn.studypilot.document.model.DocumentPageForAnalysis;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.exam.algorithm.KnowledgePointNormalizer;
import cn.studypilot.exam.algorithm.PastPaperQuestionParser;
import cn.studypilot.exam.repository.ExamAnalysisRepository;
import cn.studypilot.exam.service.PastPaperAnalysisService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PastPaperAnalysisServiceTest {
  @Test void savesOnlyProjectScopedPastPaperQuestionsAndNormalizedPoints() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class);
    ExamAnalysisRepository repository = Mockito.mock(ExamAnalysisRepository.class);
    given(documents.pastPaperPages("8", 12L)).willReturn(List.of(new DocumentPageForAnalysis(12L, 2, "1. 什么是进程？\n2. 什么条件会造成死锁？")));
    var service = new PastPaperAnalysisService(documents, new PastPaperQuestionParser(), new KnowledgePointNormalizer(), repository);

    var result = service.analyze(8L, 12L);

    assertThat(result.detectedQuestions()).isEqualTo(2);
    assertThat(result.analyzedQuestions()).isEqualTo(2);
    ArgumentCaptor<java.util.Map<cn.studypilot.exam.model.ParsedSourceQuestion, List<String>>> points = ArgumentCaptor.forClass(java.util.Map.class);
    verify(repository).replaceAnalysis(eq(8L), eq(12L), anyList(), points.capture());
    assertThat(points.getValue().values())
        .anySatisfy(value -> assertThat(value).contains("进程与线程"))
        .anySatisfy(value -> assertThat(value).contains("死锁"));
  }
}
