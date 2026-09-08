package cn.studypilot.document.service;

import cn.studypilot.common.repository.ProjectRecord;
import cn.studypilot.common.repository.ProjectRepository;
import cn.studypilot.document.dto.CreateProjectRequest;
import cn.studypilot.document.dto.ProjectResponse;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  public ProjectService(ProjectRepository projects) { this.projects = projects; }
  public ProjectResponse create(CreateProjectRequest request) {
    ProjectRecord project = projects.create(request.name().trim(), request.description() == null ? "" : request.description().trim());
    return new ProjectResponse(project.id(), project.name(), project.description(), project.createdAt());
  }
}
