package cn.studypilot.document.repository;

import cn.studypilot.document.model.DocumentChunk;
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
    return key.getKey().longValue();
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
    jdbc.update("UPDATE studypilot.documents SET status = 'READY', updated_at = now() WHERE id = :id", Map.of("id", documentId));
  }

  @Override public List<String> findReadyIndexIds(long projectId, List<String> allowedTypes) {
    return jdbc.queryForList("""
        SELECT index_id::text FROM studypilot.documents
        WHERE project_id = :projectId AND status = 'READY' AND document_type IN (:types)
        ORDER BY id
        """, Map.of("projectId", projectId, "types", allowedTypes), String.class);
  }
}
