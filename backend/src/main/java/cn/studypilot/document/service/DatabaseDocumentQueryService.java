package cn.studypilot.document.service;

import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.document.model.DocumentReference;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Resolves the evidence scope used by grounded QA.
 *
 * <p>The query deliberately excludes past papers and reference answers. Those materials are
 * reserved for the exam and assessment modules, so a question in one course cannot silently use
 * a different module's answer key.</p>
 */
@Service
public class DatabaseDocumentQueryService implements DocumentQueryService {
  private static final List<String> QA_DOCUMENT_TYPES = List.of("TEXTBOOK", "LECTURE", "KNOWLEDGE");
  private final DocumentRepository documents;

  public DatabaseDocumentQueryService(DocumentRepository documents) { this.documents = documents; }

  @Override public List<DocumentReference> availableDocuments(String projectId) {
    try {
      long id = Long.parseLong(projectId);
      if (id <= 0) throw new NumberFormatException();
      return documents.findReadyDocuments(id, QA_DOCUMENT_TYPES);
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException("课程项目编号不合法");
    }
  }
}
