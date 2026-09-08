package cn.studypilot.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.studypilot.retrieval.dto.RetrievalRequest;
import cn.studypilot.retrieval.gateway.CppRetrievalGateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Exercises the wire format independently of an actual model or C++ process. */
class CppRetrievalGatewayTest {
  @Test void mapsCppEvidenceIntoStableGatewayDto() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/retrieve", exchange -> {
      byte[] body = "{\"hits\":[{\"document_id\":\"doc-1\",\"page\":2,\"text\":\"evidence\",\"score\":0.8}]}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      var gateway = new CppRetrievalGateway(RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      var response = gateway.retrieve(new RetrievalRequest("project-1", "query", List.of("doc-1"), 5));
      assertEquals("doc-1", response.hits().getFirst().documentId());
      assertEquals(2, response.hits().getFirst().pageNumber());
    } finally { server.stop(0); }
  }
}
