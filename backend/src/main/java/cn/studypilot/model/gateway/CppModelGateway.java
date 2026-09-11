package cn.studypilot.model.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelResponse;
import cn.studypilot.model.dto.ModelTaskType;
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
    if (request.taskType() == ModelTaskType.EXAM_GENERATION) {
      body.put("response_format", examResponseFormat());
    }
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

  /** Constrains local-model decoding so exam generation cannot drift into prose or malformed JSON. */
  private Map<String, Object> examResponseFormat() {
    Map<String, Object> item = Map.of(
        "type", "object",
        "additionalProperties", false,
        "required", java.util.List.of("type", "prompt", "options", "answer", "analysis", "knowledgePoint", "score", "sourceDocumentIds"),
        "properties", Map.of(
            "type", Map.of("type", "string", "enum", java.util.List.of("SINGLE_CHOICE", "SHORT_ANSWER")),
            "prompt", Map.of("type", "string", "minLength", 1),
            "options", Map.of("type", "array", "items", Map.of("type", "string")),
            "answer", Map.of("type", "string", "minLength", 1),
            "analysis", Map.of("type", "string"),
            "knowledgePoint", Map.of("type", "string", "minLength", 1),
            "score", Map.of("type", "integer", "minimum", 1),
            "sourceDocumentIds", Map.of("type", "array", "items", Map.of("type", "string"))));
    Map<String, Object> schema = Map.of(
        "type", "array", "minItems", 2, "maxItems", 20, "items", item);
    return Map.of("type", "json_schema", "json_schema", Map.of(
        "name", "mock_exam", "strict", true, "schema", schema));
  }
}
