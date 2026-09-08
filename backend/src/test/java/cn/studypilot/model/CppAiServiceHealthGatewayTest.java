package cn.studypilot.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.model.gateway.CppAiServiceHealthGateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Contract and unavailable-service tests for the Java-to-C++ health adapter. */
class CppAiServiceHealthGatewayTest {
  @Test void mapsCppHealthPayloadIntoJavaContract() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/health", exchange -> {
      byte[] body = "{\"service\":\"StudyPilot C++\",\"model_ready\":true,\"chunks\":7,\"pending\":1,\"completed\":9}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      var gateway = new CppAiServiceHealthGateway(RestClient.builder()
          .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      var health = gateway.health();
      assertThat(health.service()).isEqualTo("StudyPilot C++");
      assertThat(health.modelReady()).isTrue();
      assertThat(health.completed()).isEqualTo(9);
    } finally { server.stop(0); }
  }

  @Test void mapsUnavailableCppServiceToStableBusinessError() {
    var gateway = new CppAiServiceHealthGateway(RestClient.builder().baseUrl("http://127.0.0.1:1").build());
    assertThatThrownBy(gateway::health).isInstanceOf(ExternalServiceException.class)
        .hasMessage("AI 服务不可用或响应超时");
  }
}
