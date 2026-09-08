package cn.studypilot.model.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP adapter for C++ {@code /health}; it maps transport names to the Java contract. */
@Component
public class CppAiServiceHealthGateway implements AiServiceHealthGateway {
  private final RestClient client;

  public CppAiServiceHealthGateway(RestClient cppAiRestClient) { this.client = cppAiRestClient; }

  @Override public AiServiceHealth health() {
    try {
      JsonNode response = client.get().uri("/health").retrieve().body(JsonNode.class);
      if (response == null || !response.hasNonNull("service") || !response.has("model_ready")) {
        throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_INVALID_RESPONSE, "AI 服务健康响应不符合协议");
      }
      return new AiServiceHealth(response.path("service").asText(), response.path("model_ready").asBoolean(),
          response.path("chunks").asInt(), response.path("pending").asInt(), response.path("completed").asLong());
    } catch (ExternalServiceException exception) { throw exception;
    } catch (RestClientException exception) {
      throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "AI 服务不可用或响应超时");
    }
  }
}
