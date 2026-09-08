package cn.studypilot.model.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** HTTP adapter for C++ /completion. It contains transport mapping, never business prompts. */
@Component
public class CppModelGateway implements ModelGateway {
  private final RestClient client;

  public CppModelGateway(RestClient cppAiRestClient) { this.client = cppAiRestClient; }

  @Override public ModelResponse complete(ModelRequest request) {
    Instant started = Instant.now();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("messages", request.messages());
    body.put("temperature", request.temperature());
    body.put("max_tokens", request.maxTokens());
    try {
      JsonNode result = client.post().uri("/completion").body(body).retrieve().body(JsonNode.class);
      if (result == null || result.path("choices").isEmpty() || result.path("choices").get(0).path("message").path("content").isMissingNode()) {
        throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_INVALID_RESPONSE, "模型服务返回内容不符合协议");
      }
      return new ModelResponse(result.path("model").asText("local-model"), result.path("choices").get(0).path("message").path("content").asText(), Duration.between(started, Instant.now()));
    } catch (ExternalServiceException error) { throw error;
    } catch (RestClientResponseException error) { throw unavailable(error.getStatusCode().value());
    } catch (RestClientException error) { throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "模型服务不可用或响应超时"); }
  }

  private ExternalServiceException unavailable(int status) {
    String message = status == 429 ? "模型服务队列繁忙，请稍后重试" : "模型服务不可用或响应超时";
    return new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, message);
  }
}
