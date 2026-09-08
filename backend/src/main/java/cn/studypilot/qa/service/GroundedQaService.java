package cn.studypilot.qa.service;

import cn.studypilot.common.exception.ResourceNotFoundException;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.model.dto.ModelMessage;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelTaskType;
import cn.studypilot.model.gateway.ModelGateway;
import cn.studypilot.qa.dto.AskQaRequest;
import cn.studypilot.qa.dto.QaAnswerResponse;
import cn.studypilot.qa.dto.QaCitationResponse;
import cn.studypilot.qa.model.QaCitationEvidence;
import cn.studypilot.qa.repository.QaRepository;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalRequest;
import cn.studypilot.retrieval.gateway.RetrievalGateway;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed RAG workflow for course materials. The model only receives evidence from the active
 * project; citations in the response are constructed from the retrieval snapshot, never model text.
 */
@Service
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class GroundedQaService {
  private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
  private final DocumentQueryService documents;
  private final RetrievalGateway retrieval;
  private final ModelGateway model;
  private final QaRepository qa;

  public GroundedQaService(DocumentQueryService documents, RetrievalGateway retrieval, ModelGateway model, QaRepository qa) {
    this.documents = documents; this.retrieval = retrieval; this.model = model; this.qa = qa;
  }

  @Transactional
  public QaAnswerResponse ask(AskQaRequest request) {
    long projectId = request.projectId();
    long sessionId = resolveSession(projectId, request.sessionId(), request.question());
    List<DocumentReference> available = documents.availableDocuments(Long.toString(projectId));
    if (available.isEmpty()) return saveInsufficient(projectId, sessionId, request.question(), "当前项目没有可用于问答的知识资料。请先上传教材、讲义或知识点资料并等待处理完成。");

    Map<String, String> names = available.stream().collect(Collectors.toMap(DocumentReference::indexId,
        DocumentReference::displayName, (first, ignored) -> first, LinkedHashMap::new));
    List<RetrievalHit> hits = retrieval.retrieve(new RetrievalRequest(Long.toString(projectId), request.question(),
        List.copyOf(names.keySet()), 3)).hits().stream()
        .filter(hit -> names.containsKey(hit.documentId()) && hit.pageNumber() > 0 && !hit.excerpt().isBlank() && hit.score() > 0)
        .limit(3).toList();
    if (hits.isEmpty()) return saveInsufficient(projectId, sessionId, request.question(), "在当前项目的知识资料中没有找到足够依据，暂不能给出确定性结论。你可以补充更相关的资料或换一种问法。");

    List<QaCitationEvidence> evidence = toEvidence(hits, names);
    String answer = model.complete(new ModelRequest(ModelTaskType.QA_ANSWER,
        prompt(qa.recentMessages(sessionId, 6), request.question(), evidence), 0.2, 600, Duration.ofSeconds(60))).content().trim();
    if (answer.isBlank()) throw new IllegalStateException("模型没有返回可用回答");
    var saved = qa.saveExchange(projectId, sessionId, request.question().trim(), answer, evidence);
    return new QaAnswerResponse(sessionId, saved.userMessageId(), saved.assistantMessageId(), "ANSWERED", answer, citations(evidence));
  }

  private long resolveSession(long projectId, Long requestedSessionId, String question) {
    if (requestedSessionId == null) return qa.createSession(projectId, title(question));
    if (!qa.belongsToProject(requestedSessionId, projectId)) throw new ResourceNotFoundException("问答会话不存在或不属于当前项目");
    return requestedSessionId;
  }

  private QaAnswerResponse saveInsufficient(long projectId, long sessionId, String question, String answer) {
    var saved = qa.saveExchange(projectId, sessionId, question.trim(), answer, List.of());
    return new QaAnswerResponse(sessionId, saved.userMessageId(), saved.assistantMessageId(), INSUFFICIENT_EVIDENCE, answer, List.of());
  }

  private List<QaCitationEvidence> toEvidence(List<RetrievalHit> hits, Map<String, String> names) {
    return java.util.stream.IntStream.range(0, hits.size()).mapToObj(index -> {
      RetrievalHit hit = hits.get(index);
      return new QaCitationEvidence(hit.documentId(), names.get(hit.documentId()), hit.pageNumber(), hit.excerpt(), hit.score(), index + 1);
    }).toList();
  }

  private List<QaCitationResponse> citations(List<QaCitationEvidence> evidence) {
    return evidence.stream().map(item -> new QaCitationResponse(item.documentName(), item.pageNumber(), item.excerpt(), item.score())).toList();
  }

  private List<ModelMessage> prompt(List<cn.studypilot.qa.model.QaHistoryMessage> history, String question,
      List<QaCitationEvidence> evidence) {
    String sources = evidence.stream().map(item -> "[资料：%s，第%d页]\n%s".formatted(item.documentName(), item.pageNumber(), item.excerpt())).collect(Collectors.joining("\n\n"));
    List<ModelMessage> messages = new java.util.ArrayList<>();
    messages.add(new ModelMessage("system", "你是课程学习助手。只能根据给出的资料证据作答；资料没有说明时必须明确说明资料不足。不要虚构资料名称、页码、事实或结论。回答使用简洁中文。\n\n资料证据：\n" + sources));
    history.forEach(item -> messages.add(new ModelMessage(item.role().equals("USER") ? "user" : "assistant", item.content())));
    messages.add(new ModelMessage("user", question.trim()));
    return List.copyOf(messages);
  }

  private String title(String question) {
    String normalized = question.trim().replaceAll("\\s+", " ");
    return normalized.length() <= 40 ? normalized : normalized.substring(0, 40) + "…";
  }
}
