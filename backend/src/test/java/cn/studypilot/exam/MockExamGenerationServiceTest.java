package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.common.repository.ProjectRepository;
import cn.studypilot.exam.model.SavedMockExam;
import cn.studypilot.exam.repository.MockExamRepository;
import cn.studypilot.exam.service.GeneratedExamItemParser;
import cn.studypilot.exam.service.MockExamGenerationService;
import cn.studypilot.exam.validation.GeneratedExamValidator;
import cn.studypilot.model.dto.ModelResponse;
import cn.studypilot.model.gateway.ModelGateway;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalResponse;
import cn.studypilot.retrieval.gateway.RetrievalGateway;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MockExamGenerationServiceTest {
  @Test void retrievesProjectEvidenceValidatesJsonAndPersistsExam() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class); RetrievalGateway retrieval = Mockito.mock(RetrievalGateway.class);
    ModelGateway model = Mockito.mock(ModelGateway.class); MockExamRepository exams = Mockito.mock(MockExamRepository.class);
    given(documents.availableDocuments("6")).willReturn(List.of(new DocumentReference("doc-1", "讲义.pdf")));
    given(retrieval.retrieve(any())).willReturn(new RetrievalResponse(List.of(new RetrievalHit("doc-1", 1, "进程是程序的一次执行。", .8))));
    given(model.complete(any())).willReturn(new ModelResponse("local", """
        [{"type":"SINGLE_CHOICE","prompt":"关于进程，哪项正确？","options":["程序的一次执行","磁盘文件"],"answer":"A","analysis":"","knowledgePoint":"进程与线程","score":5,"sourceDocumentIds":["doc-1"]},
        {"type":"SINGLE_CHOICE","prompt":"线程共享什么？","options":["进程地址空间","独立磁盘"],"answer":"A","analysis":"","knowledgePoint":"进程与线程","score":5,"sourceDocumentIds":["doc-1"]},
        {"type":"SHORT_ANSWER","prompt":"说明进程的含义。","options":[],"answer":"程序的一次执行。","analysis":"","knowledgePoint":"进程与线程","score":10,"sourceDocumentIds":["doc-1"]},
        {"type":"SHORT_ANSWER","prompt":"说明线程与进程的关系。","options":[],"answer":"线程是进程中的执行单元。","analysis":"","knowledgePoint":"进程与线程","score":10,"sourceDocumentIds":["doc-1"]}]
        """, Duration.ofMillis(10)));
    given(exams.save(eq(6L), anyString(), anyList())).willReturn(new SavedMockExam(9L, 4));
    ProjectRepository projects = Mockito.mock(ProjectRepository.class);
    var service = new MockExamGenerationService(documents, retrieval, model, projects, new GeneratedExamItemParser(new ObjectMapper()), new GeneratedExamValidator(), exams);
    assertThat(service.generate(6L, "").id()).isEqualTo(9L);
    verify(exams).save(eq(6L), anyString(), argThat(items -> items.size() == 4 && items.stream().allMatch(item -> item.sourceDocumentIds().contains("doc-1"))));
  }

  @Test void generatesFromCourseAndUserRequirementWhenThereAreNoDocuments() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class);
    RetrievalGateway retrieval = Mockito.mock(RetrievalGateway.class);
    ModelGateway model = Mockito.mock(ModelGateway.class);
    MockExamRepository exams = Mockito.mock(MockExamRepository.class);
    ProjectRepository projects = Mockito.mock(ProjectRepository.class);
    given(documents.availableDocuments("6")).willReturn(List.of());
    given(model.complete(any())).willReturn(new ModelResponse("local", """
        [{"type":"SINGLE_CHOICE","prompt":"操作系统中进程的基本状态不包括哪项？","options":["运行","就绪","阻塞","编译"],"answer":"D","analysis":"编译不是进程状态。","knowledgePoint":"进程状态","score":5,"sourceDocumentIds":[]},
        {"type":"SINGLE_CHOICE","prompt":"以下哪项属于临界区问题的目标？","options":["互斥","死锁","饥饿","轮询"],"answer":"A","analysis":"临界区要求互斥。","knowledgePoint":"进程同步","score":5,"sourceDocumentIds":[]},
        {"type":"SHORT_ANSWER","prompt":"简述进程与程序的区别。","options":[],"answer":"程序是静态代码，进程是程序的一次执行。","analysis":"","knowledgePoint":"进程概念","score":10,"sourceDocumentIds":[]},
        {"type":"SHORT_ANSWER","prompt":"说明信号量的作用。","options":[],"answer":"信号量用于实现互斥和同步。","analysis":"","knowledgePoint":"进程同步","score":10,"sourceDocumentIds":[]}]
        """, Duration.ofMillis(10)));
    given(exams.save(eq(6L), anyString(), anyList())).willReturn(new SavedMockExam(10L, 4));

    var service = new MockExamGenerationService(documents, retrieval, model, projects,
        new GeneratedExamItemParser(new ObjectMapper()), new GeneratedExamValidator(), exams);

    assertThat(service.generate(6L, "操作系统期末，难度中等").id()).isEqualTo(10L);
    verify(retrieval, times(0)).retrieve(any());
    verify(exams).save(eq(6L), anyString(), argThat(items -> items.stream()
        .allMatch(item -> item.sourceDocumentIds().isEmpty())));
  }
}
