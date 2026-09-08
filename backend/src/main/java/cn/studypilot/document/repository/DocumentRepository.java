package cn.studypilot.document.repository;

import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.model.DocumentListItem;
import cn.studypilot.document.model.RemovableDocument;
import cn.studypilot.document.model.ExtractedPage;
import java.util.List;
import java.util.UUID;

/** Persistence contract for document metadata and extracted content, scoped by project. */
public interface DocumentRepository {
  long create(long projectId, UUID indexId, String displayName, String documentType, String filePath, String contentHash);
  void replaceExtractedContent(long documentId, List<ExtractedPage> pages, List<DocumentChunk> chunks);
  /** Makes evidence visible to retrieval only after the C++ index service acknowledges it. */
  void markReady(long documentId);
  /** Keeps a failed import out of every retrieval scope while retaining its audit record. */
  void markFailed(long documentId, String reason);
  List<DocumentReference> findReadyDocuments(long projectId, List<String> allowedTypes);
  List<DocumentListItem> list(long projectId);
  RemovableDocument findRemovable(long projectId, long documentId);
  void delete(long projectId, long documentId);
}
