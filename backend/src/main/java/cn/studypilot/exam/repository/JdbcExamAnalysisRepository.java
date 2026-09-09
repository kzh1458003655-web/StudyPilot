package cn.studypilot.exam.repository;

import cn.studypilot.exam.model.KnowledgePointFrequency;
import cn.studypilot.exam.model.ParsedSourceQuestion;
import java.util.List;
import java.util.Map;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnStudyPilotDatabase
public class JdbcExamAnalysisRepository implements ExamAnalysisRepository {
  private final NamedParameterJdbcTemplate jdbc;
  public JdbcExamAnalysisRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
  @Override public void replaceAnalysis(long projectId, long documentId, List<ParsedSourceQuestion> questions,
      Map<ParsedSourceQuestion, List<String>> points) {
    jdbc.update("DELETE FROM studypilot.source_questions WHERE project_id = :projectId AND document_id = :documentId", Map.of("projectId", projectId, "documentId", documentId));
    for (ParsedSourceQuestion question : questions) {
      GeneratedKeyHolder key = new GeneratedKeyHolder();
      jdbc.update("""
          INSERT INTO studypilot.source_questions (project_id, document_id, source_page_number, question_type, stem, reference_answer, raw_source_snapshot)
          VALUES (:projectId, :documentId, :page, 'SHORT_ANSWER', :stem, '', :raw)
          """, new MapSqlParameterSource().addValue("projectId", projectId).addValue("documentId", documentId).addValue("page", question.pageNumber()).addValue("stem", question.text()).addValue("raw", question.text()), key, new String[] {"id"});
      long questionId = key.getKey().longValue();
      for (String point : points.getOrDefault(question, List.of())) jdbc.update("""
          INSERT INTO studypilot.question_knowledge_points (question_id, knowledge_point, confidence)
          VALUES (:questionId, :point, 1.0)
          """, Map.of("questionId", questionId, "point", point));
    }
  }
  @Override public List<KnowledgePointFrequency> frequencies(long projectId) {
    return jdbc.query("""
        SELECT kp.knowledge_point, count(DISTINCT kp.question_id) AS question_count
        FROM studypilot.question_knowledge_points kp JOIN studypilot.source_questions q ON q.id = kp.question_id
        WHERE q.project_id = :projectId GROUP BY kp.knowledge_point ORDER BY question_count DESC, kp.knowledge_point
        """, Map.of("projectId", projectId), (row, ignored) -> new KnowledgePointFrequency(row.getString("knowledge_point"), row.getLong("question_count")));
  }
}
