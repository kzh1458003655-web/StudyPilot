package cn.studypilot.document.service;

import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.model.DocumentPageForAnalysis;
import java.util.List;

/** 供 qa、exam、assessment 查询已校验资料的公开边界，不暴露 Repository。 */
public interface DocumentQueryService {
  List<DocumentReference> availableDocuments(String projectId);
  List<DocumentPageForAnalysis> pastPaperPages(String projectId, long documentId);
}
