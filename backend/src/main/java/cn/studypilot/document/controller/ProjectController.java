package cn.studypilot.document.controller;

import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.document.dto.CreateProjectRequest;
import cn.studypilot.document.dto.ProjectResponse;
import cn.studypilot.document.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
  private final ProjectService projects;
  public ProjectController(ProjectService projects) { this.projects = projects; }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request, HttpServletRequest http) {
    return new ApiResponse<>(projects.create(request), http.getAttribute("requestId").toString());
  }
}
