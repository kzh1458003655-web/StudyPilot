package cn.studypilot.document.controller;

import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.document.dto.CreateProjectRequest;
import cn.studypilot.document.dto.ProjectResponse;
import cn.studypilot.document.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class ProjectController {
  private final ProjectService projects;
  public ProjectController(ProjectService projects) { this.projects = projects; }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request, HttpServletRequest http) {
    return new ApiResponse<>(projects.create(request), http.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
