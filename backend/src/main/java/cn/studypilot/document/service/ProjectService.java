package cn.studypilot.document.service;

import cn.studypilot.common.repository.ProjectRecord;
import cn.studypilot.common.repository.ProjectRepository;
import cn.studypilot.common.exception.ResourceNotFoundException;
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
    return toResponse(project);
  }
  public List<ProjectResponse> list(boolean includeArchived) {
    return projects.list(includeArchived).stream().map(this::toResponse).toList();
  }
  public void archive(long projectId) {
    if (!projects.archive(projectId)) throw new ResourceNotFoundException("课程不存在或已经归档");
  }
  public void restore(long projectId) {
    if (!projects.restore(projectId)) throw new ResourceNotFoundException("课程不存在或未归档");
  }
  private ProjectResponse toResponse(ProjectRecord project) {
    return new ProjectResponse(project.id(), project.name(), project.description(), project.createdAt(), project.archivedAt());
  }
}
