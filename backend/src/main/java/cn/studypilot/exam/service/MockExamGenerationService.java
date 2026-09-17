package cn.studypilot.exam.service;

import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.common.repository.ProjectRepository;
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

/** Generates a small new exam from optional course evidence and a learner's stated requirements. */
@Service
@ConditionalOnStudyPilotDatabase
public class MockExamGenerationService {
  private final DocumentQueryService documents; private final RetrievalGateway retrieval; private final ModelGateway model; private final ProjectRepository projects;
  private final GeneratedExamItemParser parser; private final GeneratedExamValidator validator; private final MockExamRepository exams;
  public MockExamGenerationService(DocumentQueryService documents, RetrievalGateway retrieval, ModelGateway model, ProjectRepository projects, GeneratedExamItemParser parser, GeneratedExamValidator validator, MockExamRepository exams) {
    this.documents = documents; this.retrieval = retrieval; this.model = model; this.projects = projects; this.parser = parser; this.validator = validator; this.exams = exams;
  }
  @Transactional public SavedMockExam generate(long projectId, String instructions) {
    List<DocumentReference> sources = documents.availableDocuments(Long.toString(projectId));
    Set<String> ids = sources.stream().map(DocumentReference::indexId).collect(Collectors.toSet());
    Map<String, String> names = sources.stream().collect(Collectors.toMap(DocumentReference::indexId, DocumentReference::displayName));
    List<RetrievalHit> hits = ids.isEmpty() ? List.of() : retrieval.retrieve(new RetrievalRequest(
        Long.toString(projectId), "核心概念与易错知识点", List.copyOf(ids), 4)).hits().stream()
        .filter(hit -> ids.contains(hit.documentId()) && hit.score() > 0 && !hit.excerpt().isBlank()).toList();
    String evidence = hits.stream().map(hit -> "[" + hit.documentId() + " / " + names.get(hit.documentId()) + " 第" + hit.pageNumber() + "页]\n" + hit.excerpt()).collect(Collectors.joining("\n\n"));
    String courseName = projects.findById(projectId).map(project -> project.name()).orElse("当前课程");
    String learnerRequirement = instructions == null || instructions.isBlank() ? "未指定额外要求，请生成适合本科期末复习的 4 题试卷：2 道单选题和 2 道简答题。" : instructions.trim();
    String sourceRule = hits.isEmpty()
        ? "本次没有检索到可用的课程资料片段。请依据课程名称和用户要求独立出题，每题的 sourceDocumentIds 必须是空数组。"
        : "如使用下列资料，请把对应资料方括号中的 ID 写入 sourceDocumentIds；也可按课程知识独立出题，此时填空数组。不得复制原文题干。\n\n资料：\n" + evidence;
    String prompt = "为课程“" + courseName + "”生成一份全新模拟卷。用户要求：" + learnerRequirement
        + "\n只返回 JSON 数组，不要 Markdown。每项字段：type,prompt,options,answer,analysis,knowledgePoint,score,sourceDocumentIds。"
        + "题型只允许 SINGLE_CHOICE 或 SHORT_ANSWER；单选题 answer 为 A-F；简答题 answer 是参考答案。\n" + sourceRule;
    List<GeneratedExamItem> items = parser.parse(model.complete(new ModelRequest(ModelTaskType.EXAM_GENERATION,
        List.of(new ModelMessage("system", "你是严谨的课程出题助手。优先满足用户的题型、数量和难度要求；资料存在时参考资料，无资料时可使用可靠的通用课程知识。"), new ModelMessage("user", prompt)), 0.3, 1400, Duration.ofSeconds(90))).content());
    return exams.save(projectId, "模拟卷 " + java.time.LocalDate.now(), validator.validateMockExam(items, ids));
  }
}
