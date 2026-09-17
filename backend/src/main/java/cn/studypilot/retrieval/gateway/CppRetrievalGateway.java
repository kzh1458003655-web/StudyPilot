package cn.studypilot.retrieval.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalRequest;
import cn.studypilot.retrieval.dto.RetrievalResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP adapter for C++ /retrieve. Project filtering is completed before transport. */
@Component
public class CppRetrievalGateway implements RetrievalGateway {
  private final RestClient client;

  public CppRetrievalGateway(RestClient cppAiRestClient) { this.client = cppAiRestClient; }

  @Override public RetrievalResponse retrieve(RetrievalRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", request.query());
    body.put("limit", request.limit());
    body.put("document_ids", request.documentIds());
    try {
      JsonNode result = client.post().uri("/retrieve").body(body).retrieve().body(JsonNode.class);
      if (result == null || !result.path("hits").isArray()) {
        throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_INVALID_RESPONSE, "检索服务返回内容不符合协议");
      }
      List<RetrievalHit> hits = new ArrayList<>();
      for (JsonNode hit : result.path("hits")) {
        if (!hit.hasNonNull("document_id") || !hit.has("page") || !hit.hasNonNull("text")) {
          throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_INVALID_RESPONSE, "检索服务返回了无效引用片段");
        }
        hits.add(new RetrievalHit(hit.path("document_id").asText(), hit.path("page").asInt(), hit.path("text").asText(), hit.path("score").asDouble()));
      }
      return new RetrievalResponse(List.copyOf(hits));
    } catch (ExternalServiceException error) { throw error;
    } catch (RestClientException error) { throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "检索服务不可用或响应超时"); }
  }
}
