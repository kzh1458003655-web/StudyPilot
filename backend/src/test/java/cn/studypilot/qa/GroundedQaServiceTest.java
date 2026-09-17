package cn.studypilot.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelResponse;
import cn.studypilot.model.gateway.ModelGateway;
import cn.studypilot.qa.dto.AskQaRequest;
import cn.studypilot.qa.model.SavedQaExchange;
import cn.studypilot.qa.repository.QaRepository;
import cn.studypilot.qa.service.GroundedQaService;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalResponse;
import cn.studypilot.retrieval.gateway.RetrievalGateway;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class GroundedQaServiceTest {
  @Test void answersOnlyFromCurrentProjectEvidenceAndReturnsSavedCitations() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class);
    RetrievalGateway retrieval = Mockito.mock(RetrievalGateway.class);
    ModelGateway model = Mockito.mock(ModelGateway.class);
    QaRepository qa = Mockito.mock(QaRepository.class);
    given(qa.createSession(eq(7L), anyString())).willReturn(101L);
    given(documents.availableDocuments("7")).willReturn(List.of(new DocumentReference("doc-1", "操作系统讲义.pdf")));
    given(retrieval.retrieve(any())).willReturn(new RetrievalResponse(List.of(
        new RetrievalHit("doc-1", 4, "进程是程序的一次执行过程。", 0.83),
        new RetrievalHit("other-project", 1, "不应跨项目使用", 0.99))));
    given(model.complete(any())).willReturn(new ModelResponse("Qwen", "进程是程序的一次执行过程。", Duration.ofMillis(20)));
    given(qa.saveExchange(eq(7L), eq(101L), anyString(), anyString(), anyList())).willReturn(new SavedQaExchange(201L, 202L));

    var response = new GroundedQaService(documents, retrieval, model, qa).ask(new AskQaRequest(7L, null, "什么是进程？"));

    assertThat(response.status()).isEqualTo("ANSWERED");
    assertThat(response.citations()).singleElement().satisfies(citation -> {
      assertThat(citation.documentName()).isEqualTo("操作系统讲义.pdf");
      assertThat(citation.pageNumber()).isEqualTo(4);
    });
    ArgumentCaptor<ModelRequest> prompt = ArgumentCaptor.forClass(ModelRequest.class);
    verify(model).complete(prompt.capture());
    assertThat(prompt.getValue().messages().getFirst().content()).contains("操作系统讲义.pdf", "第4页").doesNotContain("不应跨项目使用");
    verify(qa).saveExchange(eq(7L), eq(101L), anyString(), anyString(), argThat(items -> items.size() == 1));
  }

  @Test void answersGeneralQuestionWithoutCourseMaterialsAndDoesNotInventCitations() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class);
    RetrievalGateway retrieval = Mockito.mock(RetrievalGateway.class);
    ModelGateway model = Mockito.mock(ModelGateway.class);
    QaRepository qa = Mockito.mock(QaRepository.class);
    given(qa.createSession(eq(7L), anyString())).willReturn(101L);
    given(documents.availableDocuments("7")).willReturn(List.of());
    given(model.complete(any())).willReturn(new ModelResponse("Qwen", "进程是程序的一次运行实例。", Duration.ofMillis(20)));
    given(qa.saveExchange(eq(7L), eq(101L), anyString(), anyString(), anyList())).willReturn(new SavedQaExchange(201L, 202L));

    var response = new GroundedQaService(documents, retrieval, model, qa).ask(new AskQaRequest(7L, null, "什么是进程？"));

    assertThat(response.status()).isEqualTo("ANSWERED");
    assertThat(response.answer()).contains("进程");
    assertThat(response.citations()).isEmpty();
    verifyNoInteractions(retrieval);
    verify(model).complete(argThat(request -> request.messages().getFirst().content().contains("尚未添加资料")));
  }

  @Test void fallsBackToGeneralKnowledgeWhenUploadedMaterialsDoNotMatchTheQuestion() {
    DocumentQueryService documents = Mockito.mock(DocumentQueryService.class);
    RetrievalGateway retrieval = Mockito.mock(RetrievalGateway.class);
    ModelGateway model = Mockito.mock(ModelGateway.class);
    QaRepository qa = Mockito.mock(QaRepository.class);
    given(qa.createSession(eq(7L), anyString())).willReturn(101L);
    given(documents.availableDocuments("7")).willReturn(List.of(new DocumentReference("doc-1", "算法讲义.pdf")));
    given(retrieval.retrieve(any())).willReturn(new RetrievalResponse(List.of()));
    given(model.complete(any())).willReturn(new ModelResponse("Qwen", "这份算法课程通常介绍复杂度、排序和图算法。", Duration.ofMillis(20)));
    given(qa.saveExchange(eq(7L), eq(101L), anyString(), anyString(), anyList())).willReturn(new SavedQaExchange(201L, 202L));

    var response = new GroundedQaService(documents, retrieval, model, qa)
        .ask(new AskQaRequest(7L, null, "这个课件讲了什么？"));

    assertThat(response.status()).isEqualTo("ANSWERED");
    assertThat(response.answer()).contains("算法课程");
    assertThat(response.citations()).isEmpty();
    verify(model).complete(argThat(request -> {
      String system = request.messages().getFirst().content();
      return system.contains("没有检索到") && system.contains("不要在回答中说明检索或引用情况");
    }));
  }

  @Test void rejectsAConversationBelongingToAnotherProject() {
    QaRepository qa = Mockito.mock(QaRepository.class);
    given(qa.belongsToProject(22L, 7L)).willReturn(false);
    var service = new GroundedQaService(Mockito.mock(DocumentQueryService.class), Mockito.mock(RetrievalGateway.class), Mockito.mock(ModelGateway.class), qa);

    assertThatThrownBy(() -> service.ask(new AskQaRequest(7L, 22L, "继续提问")))
        .hasMessage("问答会话不存在或不属于当前项目");
    verify(qa).belongsToProject(22L, 7L);
    verifyNoMoreInteractions(qa);
  }
}
