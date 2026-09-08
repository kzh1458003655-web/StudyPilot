package cn.studypilot.retrieval.gateway;

import cn.studypilot.retrieval.dto.IndexDocumentRequest;

/** Publishes and removes material from the C++ retrieval index. */
public interface DocumentIndexGateway {
  void index(IndexDocumentRequest request);
  void remove(String documentId);
}
