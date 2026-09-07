package cn.studypilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
/**
 * Java 业务层访问 C++ AI 网关的唯一出口。
 *
 * <p>前端从不接触模型端口：这样模型密钥、检索结果复核和会话持久化均能留在业务层。
 * 普通 JSON 请求和流式请求分别用 {@link #post}、{@link #stream}，但共享超时和错误语义。
 */
public class AiClient {
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper mapper;
  private final String base;
  public AiClient(ObjectMapper mapper, @Value("${study.ai}") String base) {
    this.mapper = mapper;
    this.base = base;
  }
  public HttpRequest request(String path, Object value) throws Exception {
    // 统一序列化，避免各个控制器自行拼接 JSON 或遗漏 Content-Type。
    return HttpRequest.newBuilder(URI.create(base + path))
        .timeout(Duration.ofSeconds(240))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(value)))
        .build();
  }
  public JsonNode post(String path, Object value) throws Exception {
    return checked(client.send(request(path, value), HttpResponse.BodyHandlers.ofString()));
  }
  public JsonNode get(String path) throws Exception {
    return checked(client.send(HttpRequest.newBuilder(URI.create(base + path))
                                   .timeout(Duration.ofSeconds(8))
                                   .GET()
                                   .build(),
        HttpResponse.BodyHandlers.ofString()));
  }
  public void delete(String path) throws Exception {
    checked(client.send(HttpRequest.newBuilder(URI.create(base + path))
                            .timeout(Duration.ofSeconds(10))
                            .DELETE()
                            .build(),
        HttpResponse.BodyHandlers.ofString()));
  }
  private JsonNode checked(HttpResponse<String> r) throws Exception {
    // C++ 网关把底层模型的细节收敛为 HTTP 状态；这里再转成对用户可理解的业务错误。
    if (r.statusCode() != 200)
      throw new IllegalStateException("AI 服务暂不可用，请检查本地模型或稍后重试");
    return mapper.readTree(r.body());
  }
  public HttpResponse<java.io.InputStream> stream(Object body) throws Exception {
    // 返回 InputStream 是为了让控制器逐行转发 SSE，不把完整模型回答积压在内存中。
    return client.send(request("/stream", body), HttpResponse.BodyHandlers.ofInputStream());
  }
}
