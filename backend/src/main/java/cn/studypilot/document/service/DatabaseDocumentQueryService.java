package cn.studypilot.document.service;

import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.model.DocumentPageForAnalysis;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Resolves the evidence scope used by grounded QA.
 *
 * <p>The query deliberately excludes past papers and reference answers. Those materials are
 * reserved for the exam and assessment modules, so a question in one course cannot silently use
 * a different module's answer key.</p>
 */
@Service
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
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
  @Override public List<DocumentPageForAnalysis> pastPaperPages(String projectId, long documentId) {
    try {
      long id = Long.parseLong(projectId);
      if (id <= 0 || documentId <= 0) throw new NumberFormatException();
      return documents.findPastPaperPages(id, documentId);
    } catch (NumberFormatException error) { throw new IllegalArgumentException("课程项目编号或真题资料编号不合法"); }
  }
}
