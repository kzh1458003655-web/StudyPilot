package cn.studypilot.qa.controller;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.qa.dto.AskQaRequest;
import cn.studypilot.qa.dto.QaAnswerResponse;
import cn.studypilot.qa.service.GroundedQaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;

/** Public non-streaming endpoint; the same service can later be adapted to SSE without changing RAG rules. */
@RestController
@RequestMapping("/api/v1/qa")
@ConditionalOnStudyPilotDatabase
public class QaController {
  private final GroundedQaService service;
  public QaController(GroundedQaService service) { this.service = service; }

  @PostMapping
  public ApiResponse<QaAnswerResponse> ask(@Valid @RequestBody AskQaRequest request, HttpServletRequest http) {
    return new ApiResponse<>(service.ask(request), http.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }

  @GetMapping("/history")
  public ApiResponse<cn.studypilot.qa.dto.QaHistoryResponse> history(@RequestParam long projectId,
      HttpServletRequest http) {
    return new ApiResponse<>(service.history(projectId), http.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
