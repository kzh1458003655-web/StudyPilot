package cn.studypilot.model.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the separately managed C++ retrieval and model service. */
@ConfigurationProperties(prefix = "studypilot.ai")
public record CppAiProperties(String baseUrl, Duration connectTimeout) {
  public CppAiProperties {
    baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:18081" : baseUrl;
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
  }
}
