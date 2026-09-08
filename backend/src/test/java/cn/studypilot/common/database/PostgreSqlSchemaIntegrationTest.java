package cn.studypilot.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.ExtractedPage;
import cn.studypilot.document.repository.JdbcDocumentRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.util.UUID;

/**
 * Executes the committed DDL against an isolated, temporary PostgreSQL server.
 *
 * <p>This is deliberately not an H2 compatibility test: PostgreSQL types, schema ownership,
 * foreign keys and JSONB are interpreted by PostgreSQL itself. The server exits when the test ends
 * and has no relationship to a developer or production Supabase database.</p>
 */
class PostgreSqlSchemaIntegrationTest {
  private static final List<String> SCHEMA_FILES = List.of(
      "00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql",
      "40-exam.sql", "50-assessment.sql", "90-indexes.sql");

  @Test void appliesTheCompleteSchemaAndEnforcesCoreRelationships() throws Exception {
    try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().setPort(0).start()) {
      DataSource dataSource = postgres.getPostgresDatabase();
      applySchema(dataSource);
      // A second pass models a teammate rerunning the documented schema command on the same test DB.
      applySchema(dataSource);

      try (Connection connection = dataSource.getConnection()) {
        assertThat(queryLong(connection, """
            SELECT count(*) FROM information_schema.tables
            WHERE table_schema = 'studypilot'
            """)).isEqualTo(24L);

        execute(connection, "INSERT INTO studypilot.projects (name) VALUES ('数据库测试项目')");
        JdbcDocumentRepository repository = new JdbcDocumentRepository(new NamedParameterJdbcTemplate(dataSource));
        UUID indexId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        long documentId = repository.create(1, indexId, "数据库.pdf", "LECTURE", "test.pdf", "hash-1");
        repository.replaceExtractedContent(documentId, List.of(new ExtractedPage(1, "第一页")),
            List.of(new DocumentChunk(1, 0, "测试片段", 4)));
        assertThat(repository.findReadyIndexIds(1, List.of("LECTURE"))).containsExactly(indexId.toString());

        assertThat(queryLong(connection, """
            SELECT count(*) FROM information_schema.columns
            WHERE table_schema = 'studypilot' AND table_name = 'mock_exam_items'
              AND column_name = 'options' AND data_type = 'jsonb'
            """)).isEqualTo(1L);

        execute(connection, "DELETE FROM studypilot.projects WHERE id = 1");
        assertThat(queryLong(connection, "SELECT count(*) FROM studypilot.documents"))
            .isZero();
      }
    }
  }

  private void applySchema(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      for (String file : SCHEMA_FILES) {
        ScriptUtils.executeSqlScript(connection,
            new EncodedResource(new ClassPathResource("db/schema/" + file), "UTF-8"));
      }
    }
  }

  private long queryLong(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement(); ResultSet results = statement.executeQuery(sql)) {
      results.next();
      return results.getLong(1);
    }
  }

  private void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
