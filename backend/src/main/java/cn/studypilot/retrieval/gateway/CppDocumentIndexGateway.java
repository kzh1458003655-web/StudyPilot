package cn.studypilot.retrieval.gateway;

import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.retrieval.dto.IndexDocumentRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Translates Java's document DTO into the deliberately small C++ index protocol. */
@Component
public class CppDocumentIndexGateway implements DocumentIndexGateway {
  private final RestClient client;
  public CppDocumentIndexGateway(RestClient cppAiRestClient) { this.client = cppAiRestClient; }

  @Override public void index(IndexDocumentRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", request.documentId()); body.put("name", request.displayName());
    body.put("pages", request.pages().stream().map(page -> Map.of("page", page.pageNumber(), "text", page.text())).toList());
    invoke(() -> client.post().uri("/documents").body(body).retrieve().toBodilessEntity());
  }

  @Override public void remove(String documentId) {
    invoke(() -> client.delete().uri("/documents/{id}", documentId).retrieve().toBodilessEntity());
  }

  private void invoke(TransportAction action) {
    try { action.run(); }
    catch (RestClientException exception) { throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "检索服务不可用或响应超时"); }
  }

  @FunctionalInterface private interface TransportAction { void run(); }
}
