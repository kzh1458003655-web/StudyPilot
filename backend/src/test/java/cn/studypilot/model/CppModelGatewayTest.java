package cn.studypilot.model;

import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.model.dto.ModelMessage;
import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelTaskType;
import cn.studypilot.model.gateway.CppModelGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CppModelGatewayTest {
  @Test void constrainsExamGenerationToTheExpectedJsonSchema() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/completion", exchange -> {
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] body = "{\"model\":\"local\",\"choices\":[{\"message\":{\"content\":\"[]\"}}]}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      var gateway = new CppModelGateway(RestClient.builder()
          .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      gateway.complete(new ModelRequest(ModelTaskType.EXAM_GENERATION,
          List.of(new ModelMessage("user", "出题")), .3, 1000, Duration.ofSeconds(10)));

      var json = new ObjectMapper().readTree(requestBody.get());
      assertThat(json.path("response_format").path("type").asText()).isEqualTo("json_schema");
      assertThat(json.path("response_format").path("json_schema").path("schema").path("items")
          .path("required").toString()).contains("sourceDocumentIds", "knowledgePoint");
    } finally {
      server.stop(0);
    }
  }

  @Test void constrainsShortAnswerAssessmentToValidJson() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/completion", exchange -> {
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] body = "{\"model\":\"local\",\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":8,\\\"feedback\\\":\\\"回答基本完整。\\\"}\"}}]}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      var gateway = new CppModelGateway(RestClient.builder()
          .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      gateway.complete(new ModelRequest(ModelTaskType.SHORT_ANSWER_ASSESSMENT,
          List.of(new ModelMessage("user", "评分")), 0, 300, Duration.ofSeconds(10)));

      var schema = new ObjectMapper().readTree(requestBody.get())
          .path("response_format").path("json_schema").path("schema");
      assertThat(schema.path("required").toString()).contains("score", "feedback");
      assertThat(schema.path("properties").path("score").path("type").asText())
          .isEqualTo("number");
      assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
    } finally {
      server.stop(0);
    }
  }
}
