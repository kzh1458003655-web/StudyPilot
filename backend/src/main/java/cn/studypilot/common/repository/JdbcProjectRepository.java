package cn.studypilot.common.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL implementation of the project lookup contract using named, parameterized SQL. */
@Repository
@ConditionalOnBean(NamedParameterJdbcTemplate.class)
public class JdbcProjectRepository implements ProjectRepository {
  private static final String FIND_BY_ID = """
      SELECT id, name, description, created_at, updated_at
      FROM studypilot.projects
      WHERE id = :projectId
      """;

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcProjectRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ProjectRecord> findById(long projectId) {
    return jdbc.query(FIND_BY_ID, Map.of("projectId", projectId), (resultSet, rowNumber) ->
        new ProjectRecord(
            resultSet.getLong("id"),
            resultSet.getString("name"),
            resultSet.getString("description"),
            resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
            resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()))
        .stream()
        .findFirst();
  }
}
