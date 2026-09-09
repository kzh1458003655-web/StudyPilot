package cn.studypilot.document.service;

import cn.studypilot.common.repository.ProjectRecord;
import cn.studypilot.common.repository.ProjectRepository;
import cn.studypilot.document.dto.CreateProjectRequest;
import cn.studypilot.document.dto.ProjectResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;

@Service
@ConditionalOnStudyPilotDatabase
public class ProjectService {
  private final ProjectRepository projects;
  public ProjectService(ProjectRepository projects) { this.projects = projects; }
  public ProjectResponse create(CreateProjectRequest request) {
    ProjectRecord project = projects.create(request.name().trim(), request.description() == null ? "" : request.description().trim());
    return new ProjectResponse(project.id(), project.name(), project.description(), project.createdAt());
  }
  public List<ProjectResponse> list() {
    return projects.list().stream()
        .map(project -> new ProjectResponse(project.id(), project.name(), project.description(), project.createdAt()))
        .toList();
  }
}
