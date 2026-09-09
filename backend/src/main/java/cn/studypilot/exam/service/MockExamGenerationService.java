package cn.studypilot.exam.service;

import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.exam.model.GeneratedExamItem;
import cn.studypilot.exam.model.SavedMockExam;
import cn.studypilot.exam.repository.MockExamRepository;
import cn.studypilot.exam.validation.GeneratedExamValidator;
import cn.studypilot.model.dto.ModelMessage;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelTaskType;
import cn.studypilot.model.gateway.ModelGateway;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalRequest;
import cn.studypilot.retrieval.gateway.RetrievalGateway;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates a small new exam from evidence, never copies a past-paper stem into the saved exam. */
@Service
@ConditionalOnStudyPilotDatabase
public class MockExamGenerationService {
  private final DocumentQueryService documents; private final RetrievalGateway retrieval; private final ModelGateway model;
  private final GeneratedExamItemParser parser; private final GeneratedExamValidator validator; private final MockExamRepository exams;
  public MockExamGenerationService(DocumentQueryService documents, RetrievalGateway retrieval, ModelGateway model, GeneratedExamItemParser parser, GeneratedExamValidator validator, MockExamRepository exams) {
    this.documents = documents; this.retrieval = retrieval; this.model = model; this.parser = parser; this.validator = validator; this.exams = exams;
  }
  @Transactional public SavedMockExam generate(long projectId) {
    List<DocumentReference> sources = documents.availableDocuments(Long.toString(projectId));
    if (sources.isEmpty()) throw new IllegalArgumentException("当前项目没有可用于组卷的知识资料");
    Set<String> ids = sources.stream().map(DocumentReference::indexId).collect(Collectors.toSet());
    Map<String, String> names = sources.stream().collect(Collectors.toMap(DocumentReference::indexId, DocumentReference::displayName));
    List<RetrievalHit> hits = retrieval.retrieve(new RetrievalRequest(Long.toString(projectId), "核心概念与易错知识点", List.copyOf(ids), 4)).hits().stream()
        .filter(hit -> ids.contains(hit.documentId()) && hit.score() > 0 && !hit.excerpt().isBlank()).toList();
    if (hits.isEmpty()) throw new IllegalArgumentException("知识资料中没有足够的组卷依据");
    String evidence = hits.stream().map(hit -> "[" + hit.documentId() + " / " + names.get(hit.documentId()) + " 第" + hit.pageNumber() + "页]\n" + hit.excerpt()).collect(Collectors.joining("\n\n"));
    String prompt = "根据下列资料生成 4 道全新模拟题：2 道 SINGLE_CHOICE、2 道 SHORT_ANSWER。只返回 JSON 数组，不要 Markdown。每项字段：type,prompt,options,answer,analysis,knowledgePoint,score,sourceDocumentIds。单选题 answer 为 A-F；简答题 answer 是参考答案；sourceDocumentIds 只能使用资料方括号中的 ID。不得复制原文题干。\n\n资料：\n" + evidence;
    List<GeneratedExamItem> items = parser.parse(model.complete(new ModelRequest(ModelTaskType.EXAM_GENERATION,
        List.of(new ModelMessage("system", "你是严谨的课程出题助手，只能根据给定资料出题。"), new ModelMessage("user", prompt)), 0.3, 1400, Duration.ofSeconds(90))).content());
    return exams.save(projectId, "模拟卷 " + java.time.LocalDate.now(), validator.validateMockExam(items, ids));
  }
}
