package cn.studypilot.common.repository;

import java.util.Optional;

/** The small cross-module project boundary; repositories remain inaccessible to controllers. */
public interface ProjectRepository {
  Optional<ProjectRecord> findById(long projectId);
}
