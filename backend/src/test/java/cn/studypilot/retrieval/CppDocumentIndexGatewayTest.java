package cn.studypilot.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.retrieval.dto.IndexDocumentRequest;
import cn.studypilot.retrieval.gateway.CppDocumentIndexGateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CppDocumentIndexGatewayTest {
  @Test void sendsCplusplusIndexProtocolWithPageEvidence() throws Exception {
    AtomicReference<String> body = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/documents", exchange -> {
      body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(200, -1); exchange.close();
    });
    server.start();
    try {
      var gateway = new CppDocumentIndexGateway(RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      gateway.index(new IndexDocumentRequest("123e4567-e89b-12d3-a456-426614174000", "讲义.pdf", List.of(new IndexDocumentRequest.IndexPage(2, "证据文本"))));
      assertThat(body.get()).contains("\"id\"", "\"name\"", "\"page\":2", "证据文本");
    } finally { server.stop(0); }
  }
}
