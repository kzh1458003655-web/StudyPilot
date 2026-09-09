package cn.studypilot.exam.controller;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.exam.dto.KnowledgePointFrequencyResponse;
import cn.studypilot.exam.dto.PastPaperAnalysisResponse;
import cn.studypilot.exam.dto.GeneratedExamResponse;
import cn.studypilot.exam.dto.GenerateMockExamRequest;
import cn.studypilot.exam.service.PastPaperAnalysisService;
import cn.studypilot.exam.service.MockExamGenerationService;
import cn.studypilot.exam.service.MockExamQueryService;
import jakarta.servlet.http.HttpServletRequest;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exams")
@ConditionalOnStudyPilotDatabase
public class ExamController {
  private final PastPaperAnalysisService service; private final MockExamGenerationService generation; private final MockExamQueryService query;
  public ExamController(PastPaperAnalysisService service, MockExamGenerationService generation, MockExamQueryService query) { this.service = service; this.generation = generation; this.query = query; }
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
  @PostMapping("/generate")
  public ApiResponse<GeneratedExamResponse> generate(@RequestParam long projectId,
      @RequestBody(required = false) GenerateMockExamRequest body, HttpServletRequest request) {
    var exam = generation.generate(projectId, body == null ? "" : body.normalizedInstructions());
    return new ApiResponse<>(new GeneratedExamResponse(exam.id(), exam.itemCount()), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
  @GetMapping("/{examId}")
  public ApiResponse<cn.studypilot.exam.model.MockExamDetail> find(@PathVariable long examId, @RequestParam long projectId, HttpServletRequest request) {
    return new ApiResponse<>(query.find(projectId, examId), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
  @GetMapping
  public ApiResponse<java.util.List<cn.studypilot.exam.model.MockExamSummary>> list(@RequestParam long projectId, HttpServletRequest request) {
    return new ApiResponse<>(query.list(projectId), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
