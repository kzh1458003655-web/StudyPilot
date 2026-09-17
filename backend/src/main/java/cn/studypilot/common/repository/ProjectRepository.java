package cn.studypilot.common.repository;

import java.util.List;
import java.util.Optional;

/** The small cross-module project boundary; repositories remain inaccessible to controllers. */
public interface ProjectRepository {
  ProjectRecord create(String name, String description);
  List<ProjectRecord> list(boolean includeArchived);
  Optional<ProjectRecord> findById(long projectId);
  boolean archive(long projectId);
  boolean restore(long projectId);
}
