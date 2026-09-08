package cn.studypilot.document.service;

import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.retrieval.dto.IndexDocumentRequest;
import cn.studypilot.retrieval.gateway.DocumentIndexGateway;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Coordinates one document import; controllers never talk to the repository or C++ gateway directly. */
@Service
public class DocumentImportService {
  private final DocumentIngestionService ingestion;
  private final DocumentRepository documents;
  private final DocumentIndexGateway index;

  public DocumentImportService(DocumentIngestionService ingestion, DocumentRepository documents, DocumentIndexGateway index) {
    this.ingestion = ingestion; this.documents = documents; this.index = index;
  }

  public ImportedDocument importPdf(long projectId, String displayName, String documentType, String filePath,
      String contentHash, InputStream source) throws IOException {
    UUID indexId = UUID.randomUUID();
    long documentId = documents.create(projectId, indexId, displayName, documentType, filePath, contentHash);
    try {
      var result = ingestion.ingest(source);
      documents.replaceExtractedContent(documentId, result.pages(), result.chunks());
      index.index(new IndexDocumentRequest(indexId.toString(), displayName,
          result.pages().stream().map(page -> new IndexDocumentRequest.IndexPage(page.pageNumber(), page.text())).toList()));
      documents.markReady(documentId);
      return new ImportedDocument(documentId, indexId.toString(), result.pages().size(), result.chunks().size());
    } catch (IOException | RuntimeException error) {
      documents.markFailed(documentId, error.getMessage());
      throw error;
    }
  }

  public record ImportedDocument(long documentId, String indexId, int pageCount, int chunkCount) {}
}
