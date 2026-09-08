package cn.studypilot.retrieval.dto;

import java.util.List;

/** Canonical document payload published to the internal C++ retrieval index. */
public record IndexDocumentRequest(String documentId, String displayName, List<IndexPage> pages) {
  public record IndexPage(int pageNumber, String text) {}
}
