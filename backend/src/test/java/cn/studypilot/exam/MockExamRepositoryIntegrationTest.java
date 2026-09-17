package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.exam.model.GeneratedExamItem;
import cn.studypilot.exam.repository.JdbcMockExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class MockExamRepositoryIntegrationTest {
  private static final EmbeddedPostgres POSTGRES = startPostgres();
  private static DataSource dataSource;

  @BeforeAll static void schema() throws Exception {
    dataSource = POSTGRES.getPostgresDatabase();
    try (Connection connection = dataSource.getConnection()) {
      for (String script : List.of("00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql",
          "40-exam.sql", "50-assessment.sql", "60-project-archive.sql", "90-indexes.sql")) {
        ScriptUtils.executeSqlScript(connection,
            new org.springframework.core.io.ClassPathResource("db/schema/" + script));
      }
    }
  }

  @AfterAll static void stop() throws Exception { POSTGRES.close(); }

  @Test void projectScopedDeleteCascadesAttemptsAndAnswers() {
    JdbcTemplate sql = new JdbcTemplate(dataSource);
    long owner = sql.queryForObject("INSERT INTO studypilot.projects(name) VALUES ('owner') RETURNING id", Long.class);
    long other = sql.queryForObject("INSERT INTO studypilot.projects(name) VALUES ('other') RETURNING id", Long.class);
    var repository = new JdbcMockExamRepository(new NamedParameterJdbcTemplate(dataSource), new ObjectMapper());
    long examId = repository.save(owner, "练习", List.of(new GeneratedExamItem(
        "SINGLE_CHOICE", "事务的原子性指什么？", List.of("全部成功或回滚", "永久保存"), "A", "",
        "事务", 5, List.of()))).id();
    long itemId = sql.queryForObject("SELECT id FROM studypilot.mock_exam_items WHERE exam_id = ?", Long.class, examId);
    long attemptId = sql.queryForObject("INSERT INTO studypilot.exam_attempts(exam_id,status) VALUES (?, 'IN_PROGRESS') RETURNING id", Long.class, examId);
    sql.update("INSERT INTO studypilot.attempt_answers(attempt_id,item_id,answer,grading_status) VALUES (?, ?, 'A', 'PENDING')",
        attemptId, itemId);

    assertThat(repository.deleteByProject(other, examId)).isFalse();
    assertThat(sql.queryForObject("SELECT count(*) FROM studypilot.mock_exams WHERE id = ?", Integer.class, examId)).isEqualTo(1);
    assertThat(repository.deleteByProject(owner, examId)).isTrue();
    assertThat(sql.queryForObject("SELECT count(*) FROM studypilot.exam_attempts WHERE id = ?", Integer.class, attemptId)).isZero();
    assertThat(sql.queryForObject("SELECT count(*) FROM studypilot.attempt_answers WHERE attempt_id = ?", Integer.class, attemptId)).isZero();
  }

  private static EmbeddedPostgres startPostgres() {
    try { return EmbeddedPostgres.builder().setPort(0).start(); }
    catch (java.io.IOException error) { throw new IllegalStateException(error); }
  }
}
