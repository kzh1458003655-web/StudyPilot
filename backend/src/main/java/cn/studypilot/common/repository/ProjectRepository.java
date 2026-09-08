package cn.studypilot.common.repository;

import java.util.Optional;

/** The small cross-module project boundary; repositories remain inaccessible to controllers. */
public interface ProjectRepository {
  ProjectRecord create(String name, String description);
  Optional<ProjectRecord> findById(long projectId);
}
