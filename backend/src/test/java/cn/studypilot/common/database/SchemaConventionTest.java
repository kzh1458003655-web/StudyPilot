package cn.studypilot.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Guards the portable PostgreSQL conventions before an operator runs the DDL on a real server. */
class SchemaConventionTest {
  private static final List<String> FILES = List.of(
      "00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql",
      "40-exam.sql", "50-assessment.sql", "90-indexes.sql");

  @Test void schemaFilesUseTargetSchemaAndAvoidLegacyMysqlSyntax() throws IOException {
    for (String file : FILES) {
      String sql = new ClassPathResource("db/schema/" + file)
          .getContentAsString(StandardCharsets.UTF_8)
          .toUpperCase();
      assertThat(sql).contains("STUDYPILOT");
      assertThat(sql).doesNotContain("AUTO_INCREMENT", "DATETIME", "MEDIUMTEXT", "ON DUPLICATE KEY UPDATE");
    }
  }

  @Test void schemaContainsAllModuleTablesAndPostgresqlTypes() throws IOException {
    String sql = String.join("\n", FILES.stream()
        .map(this::readSchema)
        .toList());
    assertThat(sql).contains(
        "document_chunks", "document_index_status", "qa_retrieval_records",
        "source_questions", "question_knowledge_points", "grading_details",
        "recommendation_records", "TIMESTAMPTZ", "JSONB");
  }

  private String readSchema(String file) {
    try {
      return new ClassPathResource("db/schema/" + file).getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read schema resource: " + file, exception);
    }
  }
}
