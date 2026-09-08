package cn.studypilot.document.service;

import cn.studypilot.document.model.DocumentListItem;
import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.retrieval.gateway.DocumentIndexGateway;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

/** Read and deletion workflow for the resource centre, always constrained by its project id. */
@Service
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class DocumentResourceService {
  private final DocumentRepository documents;
  private final DocumentIndexGateway index;
  private final LocalDocumentStorage storage;
  public DocumentResourceService(DocumentRepository documents, DocumentIndexGateway index, LocalDocumentStorage storage) {
    this.documents = documents; this.index = index; this.storage = storage;
  }
  public List<DocumentListItem> list(long projectId) { return documents.list(projectId); }
  @Transactional
  public void delete(long projectId, long documentId) throws IOException {
    var document = documents.findRemovable(projectId, documentId);
    index.remove(document.indexId());
    documents.delete(projectId, documentId);
    storage.delete(document.filePath());
  }
}
