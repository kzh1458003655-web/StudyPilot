package cn.studypilot.qa.repository;

import cn.studypilot.qa.model.QaCitationEvidence;
import cn.studypilot.qa.model.QaHistoryMessage;
import cn.studypilot.qa.model.SavedQaExchange;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Parameterized PostgreSQL persistence for a complete QA exchange and its citation snapshots. */
@Repository
@ConditionalOnBean(NamedParameterJdbcTemplate.class)
public class JdbcQaRepository implements QaRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public JdbcQaRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override public long createSession(long projectId, String title) {
    return insertReturningId("INSERT INTO studypilot.qa_sessions (project_id, title) VALUES (:projectId, :title)",
        new MapSqlParameterSource().addValue("projectId", projectId).addValue("title", title));
  }

  @Override public boolean belongsToProject(long sessionId, long projectId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM studypilot.qa_sessions WHERE id = :sessionId AND project_id = :projectId)
        """, Map.of("sessionId", sessionId, "projectId", projectId), Boolean.class));
  }

  @Override public List<QaHistoryMessage> recentMessages(long sessionId, int limit) {
    return jdbc.query("""
        SELECT role, content FROM (
          SELECT id, role, content FROM studypilot.qa_messages WHERE session_id = :sessionId ORDER BY id DESC LIMIT :limit
        ) recent ORDER BY id
        """, Map.of("sessionId", sessionId, "limit", limit), (row, ignored) ->
        new QaHistoryMessage(row.getString("role"), row.getString("content")));
  }

  @Override public SavedQaExchange saveExchange(long projectId, long sessionId, String question, String answer,
      List<QaCitationEvidence> evidence) {
    long userId = insertMessage(sessionId, "USER", question);
    long assistantId = insertMessage(sessionId, "ASSISTANT", answer);
    for (QaCitationEvidence item : evidence) saveEvidence(projectId, assistantId, item);
    jdbc.update("UPDATE studypilot.qa_sessions SET updated_at = now() WHERE id = :id", Map.of("id", sessionId));
    return new SavedQaExchange(userId, assistantId);
  }

  private long insertMessage(long sessionId, String role, String content) {
    return insertReturningId("""
        INSERT INTO studypilot.qa_messages (session_id, role, content) VALUES (:sessionId, :role, :content)
        """, new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("role", role).addValue("content", content));
  }

  private void saveEvidence(long projectId, long assistantId, QaCitationEvidence item) {
    Long documentId = jdbc.query("""
        SELECT id FROM studypilot.documents WHERE project_id = :projectId AND index_id::text = :indexId
        """, Map.of("projectId", projectId, "indexId", item.documentIndexId()), (row, ignored) -> row.getLong("id"))
        .stream().findFirst().orElse(null);
    long retrievalId = insertReturningId("""
        INSERT INTO studypilot.qa_retrieval_records
          (assistant_message_id, document_id, page_number, rank_position, score, excerpt_snapshot)
        VALUES (:assistantId, :documentId, :pageNumber, :rank, :score, :excerpt)
        """, new MapSqlParameterSource().addValue("assistantId", assistantId).addValue("documentId", documentId)
        .addValue("pageNumber", item.pageNumber()).addValue("rank", item.rankPosition())
        .addValue("score", item.score()).addValue("excerpt", item.excerpt()));
    jdbc.update("""
        INSERT INTO studypilot.qa_citations
          (assistant_message_id, retrieval_record_id, document_id, document_name_snapshot, page_number, excerpt_snapshot)
        VALUES (:assistantId, :retrievalId, :documentId, :documentName, :pageNumber, :excerpt)
        """, new MapSqlParameterSource().addValue("assistantId", assistantId).addValue("retrievalId", retrievalId)
        .addValue("documentId", documentId).addValue("documentName", item.documentName())
        .addValue("pageNumber", item.pageNumber()).addValue("excerpt", item.excerpt()));
  }

  private long insertReturningId(String sql, MapSqlParameterSource parameters) {
    KeyHolder key = new GeneratedKeyHolder();
    jdbc.update(sql, parameters, key, new String[] {"id"});
    return key.getKey().longValue();
  }
}
