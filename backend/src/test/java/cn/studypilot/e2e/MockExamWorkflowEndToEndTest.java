package cn.studypilot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.model.dto.ModelResponse;
import cn.studypilot.model.gateway.ModelGateway;
import cn.studypilot.retrieval.dto.RetrievalHit;
import cn.studypilot.retrieval.dto.RetrievalResponse;
import cn.studypilot.retrieval.gateway.DocumentIndexGateway;
import cn.studypilot.retrieval.gateway.RetrievalGateway;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Opt-in HTTP workflow test. It uses real Spring MVC, PostgreSQL and C++ PDF indexing, while
 * controlled retrieval/model gateways make the strict JSON-generation assertion reproducible.
 */
@Tag("e2e")
@EnabledIfSystemProperty(named = "studypilot.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MockExamWorkflowEndToEndTest.ControlledGateways.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockExamWorkflowEndToEndTest {
  private static final EmbeddedPostgres POSTGRES = startPostgres();
  private static final AtomicReference<String> SOURCE_ID = new AtomicReference<>();
  private static final List<String> SCRIPTS = List.of("00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql", "40-exam.sql", "50-assessment.sql", "90-indexes.sql");
  @LocalServerPort int port;
  @Autowired TestRestTemplate http;

  @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
    properties.add("studypilot.database.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
    properties.add("studypilot.database.username", () -> "postgres");
    properties.add("studypilot.database.password", () -> "local-test");
  }
  @BeforeAll void schema() throws Exception {
    DataSource source = POSTGRES.getPostgresDatabase();
    try (Connection connection = source.getConnection()) {
      for (String script : SCRIPTS) ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.ClassPathResource("db/schema/" + script));
    }
  }
  @AfterAll static void stopDatabase() throws Exception { POSTGRES.close(); }

  @Test void createsAndReadsAValidatedFourQuestionMockExam() throws Exception {
    int projectId = ((Number) ((Map<?, ?>) http.postForEntity(url("/api/v1/projects"), Map.of("name", "模拟卷端到端测试", "description", ""), Map.class).getBody().get("data")).get("id")).intValue();
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("projectId", Integer.toString(projectId)); form.add("documentType", "LECTURE");
    form.add("file", new ByteArrayResource(pdf("A process is an executing program. A thread is an execution unit.")) { @Override public String getFilename() { return "notes.pdf"; } });
    HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    ResponseEntity<Map> imported = http.postForEntity(url("/api/v1/documents"), new HttpEntity<>(form, headers), Map.class);
    assertThat(imported.getStatusCode().value()).isEqualTo(201);
    Map<?, ?> importedData = (Map<?, ?>) imported.getBody().get("data");
    int documentId = ((Number) importedData.get("id")).intValue();
    SOURCE_ID.set((String) importedData.get("indexId"));

    ResponseEntity<Map> generated = http.postForEntity(url("/api/v1/exams/generate?projectId=" + projectId), null, Map.class);
    assertThat(generated.getStatusCode().is2xxSuccessful()).isTrue();
    int examId = ((Number) ((Map<?, ?>) generated.getBody().get("data")).get("examId")).intValue();
    ResponseEntity<Map> paper = http.getForEntity(url("/api/v1/exams/" + examId + "?projectId=" + projectId), Map.class);
    assertThat(paper.getStatusCode().is2xxSuccessful()).isTrue();
    List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map<?, ?>) paper.getBody().get("data")).get("items");
    assertThat(items).hasSize(4);
    ResponseEntity<Map> started = http.postForEntity(url("/api/v1/assessments/attempts?projectId=" + projectId + "&examId=" + examId), null, Map.class);
    assertThat(started.getStatusCode().is2xxSuccessful()).isTrue();
    int attemptId = ((Number) ((Map<?, ?>) started.getBody().get("data")).get("attemptId")).intValue();
    List<Map<String, Object>> answers = List.of(
        Map.of("itemId", items.get(0).get("id"), "answer", "A"), Map.of("itemId", items.get(1).get("id"), "answer", "B"),
        Map.of("itemId", items.get(2).get("id"), "answer", "An executing program."), Map.of("itemId", items.get(3).get("id"), "answer", "An execution unit."));
    ResponseEntity<Map> assessed = http.postForEntity(url("/api/v1/assessments/attempts/" + attemptId + "/submit?projectId=" + projectId), Map.of("answers", answers), Map.class);
    assertThat(assessed.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(((List<?>) ((Map<?, ?>) assessed.getBody().get("data")).get("answers"))).hasSize(4);
    assertThat(http.exchange(url("/api/v1/documents/" + documentId + "?projectId=" + projectId), org.springframework.http.HttpMethod.DELETE, null, Void.class)
        .getStatusCode().value()).isEqualTo(204);
  }

  private String url(String path) { return "http://127.0.0.1:" + port + path; }
  private static EmbeddedPostgres startPostgres() { try { return EmbeddedPostgres.builder().setPort(0).start(); } catch (java.io.IOException error) { throw new IllegalStateException(error); } }
  private byte[] pdf(String text) throws Exception {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage()); try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
        content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12); content.newLineAtOffset(72, 720); content.showText(text); content.endText(); }
      document.save(output); return output.toByteArray();
    }
  }

  @TestConfiguration static class ControlledGateways {
    @Bean @Primary DocumentIndexGateway documentIndexGateway() { return new DocumentIndexGateway() {
      @Override public void index(cn.studypilot.retrieval.dto.IndexDocumentRequest request) { }
      @Override public void remove(String documentId) { }
    }; }
    @Bean @Primary RetrievalGateway retrievalGateway() { return request -> new RetrievalResponse(List.of(new RetrievalHit(SOURCE_ID.get(), 1, "A process is an executing program.", 0.9))); }
    @Bean @Primary ModelGateway modelGateway() { return request -> new ModelResponse("controlled-e2e",
        request.taskType() == cn.studypilot.model.dto.ModelTaskType.SHORT_ANSWER_ASSESSMENT ? "{\"score\":10,\"feedback\":\"答案完整。\"}" : json(SOURCE_ID.get()), Duration.ofMillis(1)); }
    private static String json(String id) { return """
        [{"type":"SINGLE_CHOICE","prompt":"Which statement describes a process?","options":["An executing program","A static file"],"answer":"A","analysis":"","knowledgePoint":"process","score":5,"sourceDocumentIds":["%s"]},
        {"type":"SINGLE_CHOICE","prompt":"Which unit executes inside a process?","options":["A thread","A disk"],"answer":"A","analysis":"","knowledgePoint":"thread","score":5,"sourceDocumentIds":["%s"]},
        {"type":"SHORT_ANSWER","prompt":"Define a process.","options":[],"answer":"An executing program.","analysis":"","knowledgePoint":"process","score":10,"sourceDocumentIds":["%s"]},
        {"type":"SHORT_ANSWER","prompt":"Define a thread.","options":[],"answer":"An execution unit.","analysis":"","knowledgePoint":"thread","score":10,"sourceDocumentIds":["%s"]}]
        """.formatted(id, id, id, id); }
  }
}
