package cn.studypilot.document.repository;

import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.ExtractedPage;
import java.util.List;
import java.util.UUID;

/** Persistence contract for document metadata and extracted content, scoped by project. */
public interface DocumentRepository {
  long create(long projectId, UUID indexId, String displayName, String documentType, String filePath, String contentHash);
  void replaceExtractedContent(long documentId, List<ExtractedPage> pages, List<DocumentChunk> chunks);
  List<String> findReadyIndexIds(long projectId, List<String> allowedTypes);
}
