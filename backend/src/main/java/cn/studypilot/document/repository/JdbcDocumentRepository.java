package cn.studypilot.document.repository;

import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.model.DocumentListItem;
import cn.studypilot.document.model.RemovableDocument;
import cn.studypilot.document.model.DocumentPageForAnalysis;
import cn.studypilot.common.exception.ResourceNotFoundException;
import cn.studypilot.document.model.ExtractedPage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** PostgreSQL implementation; every read that feeds retrieval includes the project boundary. */
@Repository
@ConditionalOnBean(NamedParameterJdbcTemplate.class)
public class JdbcDocumentRepository implements DocumentRepository {
  private final NamedParameterJdbcTemplate jdbc;
  public JdbcDocumentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override public long create(long projectId, UUID indexId, String displayName, String documentType, String filePath, String contentHash) {
    KeyHolder key = new GeneratedKeyHolder();
    MapSqlParameterSource parameters = new MapSqlParameterSource()
        .addValue("projectId", projectId).addValue("indexId", indexId).addValue("displayName", displayName)
        .addValue("documentType", documentType).addValue("filePath", filePath).addValue("contentHash", contentHash);
    jdbc.update("""
        INSERT INTO studypilot.documents (project_id, index_id, display_name, document_type, file_path, content_hash, status)
        VALUES (:projectId, :indexId, :displayName, :documentType, :filePath, :contentHash, 'PROCESSING')
        """, parameters, key, new String[] {"id"});
    long documentId = key.getKey().longValue();
    jdbc.update("""
        INSERT INTO studypilot.document_index_status (document_id, status)
        VALUES (:id, 'PENDING')
        """, Map.of("id", documentId));
    return documentId;
  }

  @Override public void replaceExtractedContent(long documentId, List<ExtractedPage> pages, List<DocumentChunk> chunks) {
    jdbc.update("DELETE FROM studypilot.document_pages WHERE document_id = :id", Map.of("id", documentId));
    for (ExtractedPage page : pages) jdbc.update("""
        INSERT INTO studypilot.document_pages (document_id, page_number, text_content)
        VALUES (:documentId, :pageNumber, :text)
        """, Map.of("documentId", documentId, "pageNumber", page.pageNumber(), "text", page.text()));
    Map<Integer, Long> pageIds = new HashMap<>();
    RowCallbackHandler collectPageIds = rs -> pageIds.put(rs.getInt("page_number"), rs.getLong("id"));
    jdbc.query("SELECT id, page_number FROM studypilot.document_pages WHERE document_id = :documentId", Map.of("documentId", documentId), collectPageIds);
    for (DocumentChunk chunk : chunks) jdbc.update("""
        INSERT INTO studypilot.document_chunks (document_id, page_id, chunk_index, text_content, token_count, content_hash)
        VALUES (:documentId, :pageId, :chunkIndex, :text, :tokenCount, :contentHash)
        """, Map.of("documentId", documentId, "pageId", pageIds.get(chunk.pageNumber()), "chunkIndex", chunk.chunkIndex(), "text", chunk.text(), "tokenCount", chunk.tokenEstimate(), "contentHash", Integer.toHexString(chunk.text().hashCode())));
    jdbc.update("""
        UPDATE studypilot.document_index_status
        SET status = 'INDEXING', chunk_count = :chunkCount, error_message = '', updated_at = now()
        WHERE document_id = :id
        """, Map.of("id", documentId, "chunkCount", chunks.size()));
  }

  @Override public void markReady(long documentId) {
    jdbc.update("UPDATE studypilot.documents SET status = 'READY', updated_at = now() WHERE id = :id", Map.of("id", documentId));
    jdbc.update("""
        UPDATE studypilot.document_index_status
        SET status = 'READY', error_message = '', updated_at = now()
        WHERE document_id = :id
        """, Map.of("id", documentId));
  }

  @Override public void markFailed(long documentId, String reason) {
    String safeReason = reason == null || reason.isBlank() ? "资料处理失败" : reason.substring(0, Math.min(reason.length(), 500));
    jdbc.update("UPDATE studypilot.documents SET status = 'FAILED', updated_at = now() WHERE id = :id", Map.of("id", documentId));
    jdbc.update("""
        UPDATE studypilot.document_index_status
        SET status = 'FAILED', error_message = :reason, updated_at = now()
        WHERE document_id = :id
        """, Map.of("id", documentId, "reason", safeReason));
  }

  @Override public List<DocumentReference> findReadyDocuments(long projectId, List<String> allowedTypes) {
    return jdbc.query("""
        SELECT index_id::text, display_name FROM studypilot.documents
        WHERE project_id = :projectId AND status = 'READY' AND document_type IN (:types)
        ORDER BY id
        """, Map.of("projectId", projectId, "types", allowedTypes), (row, ignored) ->
        new DocumentReference(row.getString("index_id"), row.getString("display_name")));
  }

  @Override public List<DocumentListItem> list(long projectId) {
    return jdbc.query("""
        SELECT d.id, d.display_name, d.document_type, d.status,
          (SELECT count(*) FROM studypilot.document_pages p WHERE p.document_id = d.id) AS page_count,
          (SELECT count(*) FROM studypilot.document_chunks c WHERE c.document_id = d.id) AS chunk_count
        FROM studypilot.documents d WHERE d.project_id = :projectId ORDER BY d.id DESC
        """, Map.of("projectId", projectId), (row, ignored) -> new DocumentListItem(row.getLong("id"),
        row.getString("display_name"), row.getString("document_type"), row.getString("status"), row.getInt("page_count"), row.getInt("chunk_count")));
  }

  @Override public RemovableDocument findRemovable(long projectId, long documentId) {
    return jdbc.query("""
        SELECT id, index_id::text, file_path FROM studypilot.documents
        WHERE id = :documentId AND project_id = :projectId
        """, Map.of("projectId", projectId, "documentId", documentId), (row, ignored) ->
        new RemovableDocument(row.getLong("id"), row.getString("index_id"), row.getString("file_path")))
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("资料不存在或不属于当前项目"));
  }

  @Override public void delete(long projectId, long documentId) {
    jdbc.update("DELETE FROM studypilot.documents WHERE id = :documentId AND project_id = :projectId",
        Map.of("projectId", projectId, "documentId", documentId));
  }

  @Override public List<DocumentPageForAnalysis> findPastPaperPages(long projectId, long documentId) {
    return jdbc.query("""
        SELECT d.id AS document_id, p.page_number, p.text_content
        FROM studypilot.documents d JOIN studypilot.document_pages p ON p.document_id = d.id
        WHERE d.id = :documentId AND d.project_id = :projectId AND d.document_type = 'PAST_EXAM' AND d.status = 'READY'
        ORDER BY p.page_number
        """, Map.of("projectId", projectId, "documentId", documentId), (row, ignored) ->
        new DocumentPageForAnalysis(row.getLong("document_id"), row.getInt("page_number"), row.getString("text_content")));
  }
}
