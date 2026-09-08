package cn.studypilot.exam.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.studypilot.exam.model.GeneratedExamItem;
import cn.studypilot.exam.model.MockExamDetail;
import cn.studypilot.exam.model.MockExamItemView;
import cn.studypilot.exam.model.SavedMockExam;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class JdbcMockExamRepository implements MockExamRepository {
  private final NamedParameterJdbcTemplate jdbc; private final ObjectMapper json;
  public JdbcMockExamRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
  @Override public SavedMockExam save(long projectId, String title, List<GeneratedExamItem> items) {
    GeneratedKeyHolder examKey = new GeneratedKeyHolder();
    jdbc.update("INSERT INTO studypilot.mock_exams (project_id, title) VALUES (:projectId, :title)", new MapSqlParameterSource().addValue("projectId", projectId).addValue("title", title), examKey, new String[] {"id"});
    long examId = examKey.getKey().longValue(); int ordinal = 1;
    for (GeneratedExamItem item : items) {
      GeneratedKeyHolder itemKey = new GeneratedKeyHolder();
      jdbc.update("""
          INSERT INTO studypilot.mock_exam_items (exam_id, ordinal, question_type, prompt, options, answer, analysis, knowledge_point, score)
          VALUES (:examId, :ordinal, :type, :prompt, CAST(:options AS jsonb), :answer, :analysis, :point, :score)
          """, new MapSqlParameterSource().addValue("examId", examId).addValue("ordinal", ordinal++).addValue("type", item.type())
          .addValue("prompt", item.prompt()).addValue("options", serialize(item.options())).addValue("answer", item.answer())
          .addValue("analysis", item.analysis() == null ? "" : item.analysis()).addValue("point", item.knowledgePoint()).addValue("score", item.score()), itemKey, new String[] {"id"});
      long itemId = itemKey.getKey().longValue();
      jdbc.update("""
          INSERT INTO studypilot.question_validation_records (exam_item_id, validation_type, passed, details)
          VALUES (:itemId, 'STRUCTURED_OUTPUT', true, CAST(:details AS jsonb))
          """, new MapSqlParameterSource().addValue("itemId", itemId).addValue("details", "{\"validated\":true}"));
    }
    return new SavedMockExam(examId, items.size());
  }
  @Override public Optional<MockExamDetail> findByProject(long projectId, long examId) {
    var header = jdbc.query("SELECT id, title FROM studypilot.mock_exams WHERE id = :examId AND project_id = :projectId",
        Map.of("examId", examId, "projectId", projectId), (row, ignored) -> new Object[] {row.getLong("id"), row.getString("title")});
    if (header.isEmpty()) return Optional.empty();
    List<MockExamItemView> items = jdbc.query("""
        SELECT id, ordinal, question_type, prompt, options::text, answer, analysis, knowledge_point, score
        FROM studypilot.mock_exam_items WHERE exam_id = :examId ORDER BY ordinal
        """, Map.of("examId", examId), (row, ignored) -> new MockExamItemView(row.getLong("id"), row.getInt("ordinal"),
        row.getString("question_type"), row.getString("prompt"), deserialize(row.getString("options")), row.getString("answer"),
        row.getString("analysis"), row.getString("knowledge_point"), row.getDouble("score")));
    Object[] record = header.getFirst();
    return Optional.of(new MockExamDetail((long) record[0], (String) record[1], items));
  }
  private String serialize(List<String> options) { try { return json.writeValueAsString(options); } catch (Exception error) { throw new IllegalStateException(error); } }
  private List<String> deserialize(String options) { try { return json.readValue(options, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}); } catch (Exception error) { throw new IllegalStateException(error); } }
}
