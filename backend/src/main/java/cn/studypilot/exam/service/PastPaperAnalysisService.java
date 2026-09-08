package cn.studypilot.exam.service;

import cn.studypilot.document.model.ExtractedPage;
import cn.studypilot.document.service.DocumentQueryService;
import cn.studypilot.exam.algorithm.KnowledgePointNormalizer;
import cn.studypilot.exam.algorithm.PastPaperQuestionParser;
import cn.studypilot.exam.model.KnowledgePointFrequency;
import cn.studypilot.exam.repository.ExamAnalysisRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fixed workflow: extract only reliable text questions, normalize points, then replace this paper's analysis. */
@Service
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class PastPaperAnalysisService {
  private final DocumentQueryService documents; private final PastPaperQuestionParser parser;
  private final KnowledgePointNormalizer normalizer; private final ExamAnalysisRepository repository;
  public PastPaperAnalysisService(DocumentQueryService documents, PastPaperQuestionParser parser, KnowledgePointNormalizer normalizer, ExamAnalysisRepository repository) {
    this.documents = documents; this.parser = parser; this.normalizer = normalizer; this.repository = repository;
  }
  @Transactional public AnalysisResult analyze(long projectId, long documentId) {
    var pages = documents.pastPaperPages(Long.toString(projectId), documentId);
    if (pages.isEmpty()) throw new IllegalArgumentException("未找到可分析的已就绪历年真题资料");
    var questions = parser.parse(pages.stream().map(page -> new ExtractedPage(page.pageNumber(), page.text())).toList());
    Map<Integer, List<String>> points = questions.stream().collect(Collectors.toMap(question -> question.ordinal(), question -> normalizer.extractAndNormalize(question.text())));
    repository.replaceAnalysis(projectId, documentId, questions, points);
    long analyzed = points.values().stream().filter(list -> !list.isEmpty()).count();
    return new AnalysisResult(questions.size(), analyzed, questions.size() - analyzed);
  }
  public List<KnowledgePointFrequency> frequencies(long projectId) { return repository.frequencies(projectId); }
  public record AnalysisResult(int detectedQuestions, long analyzedQuestions, long needsReviewQuestions) {}
}
