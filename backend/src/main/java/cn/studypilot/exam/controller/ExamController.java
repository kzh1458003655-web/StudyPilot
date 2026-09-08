package cn.studypilot.exam.controller;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.exam.dto.KnowledgePointFrequencyResponse;
import cn.studypilot.exam.dto.PastPaperAnalysisResponse;
import cn.studypilot.exam.service.PastPaperAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exams")
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class ExamController {
  private final PastPaperAnalysisService service;
  public ExamController(PastPaperAnalysisService service) { this.service = service; }
  @PostMapping("/past-papers/{documentId}/analysis")
  public ApiResponse<PastPaperAnalysisResponse> analyze(@PathVariable long documentId, @RequestParam long projectId, HttpServletRequest request) {
    var result = service.analyze(projectId, documentId);
    return new ApiResponse<>(new PastPaperAnalysisResponse(result.detectedQuestions(), result.analyzedQuestions(), result.needsReviewQuestions()), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
  @GetMapping("/knowledge-points")
  public ApiResponse<java.util.List<KnowledgePointFrequencyResponse>> frequencies(@RequestParam long projectId, HttpServletRequest request) {
    var data = service.frequencies(projectId).stream().map(item -> new KnowledgePointFrequencyResponse(item.knowledgePoint(), item.questionCount())).toList();
    return new ApiResponse<>(data, request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
