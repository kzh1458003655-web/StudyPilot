package cn.studypilot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.util.List;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
 * Opt-in product proof: run with {@code -Dstudypilot.e2e=true}. It requires the local C++ service
 * and its model service, but creates and removes its own PostgreSQL data.
 */
@Tag("e2e")
@EnabledIfSystemProperty(named = "studypilot.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroundedQaEndToEndTest {
  private static final EmbeddedPostgres POSTGRES = startPostgres();
  private static final List<String> SCRIPTS = List.of("00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql", "40-exam.sql", "50-assessment.sql", "90-indexes.sql");
  @LocalServerPort int port;
  @Autowired TestRestTemplate http;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("studypilot.database.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
    properties.add("studypilot.database.username", () -> "postgres");
    properties.add("studypilot.database.password", () -> "local-test");
  }

  @BeforeAll void schema() throws Exception {
    DataSource source = POSTGRES.getPostgresDatabase();
    try (Connection connection = source.getConnection()) {
      for (String script : SCRIPTS) {
        ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.ClassPathResource("db/schema/" + script));
      }
    }
  }

  @AfterAll static void stopDatabase() throws Exception { POSTGRES.close(); }

  @Test void createsProjectImportsPdfAndReturnsTraceableAnswer() throws Exception {
    ResponseEntity<java.util.Map> project = http.postForEntity(url("/api/v1/projects"),
        java.util.Map.of("name", "端到端测试课程", "description", ""), java.util.Map.class);
    assertThat(project.getStatusCode().is2xxSuccessful()).isTrue();
    int projectId = ((Number) ((java.util.Map<?, ?>) project.getBody().get("data")).get("id")).intValue();

    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("projectId", Integer.toString(projectId)); form.add("documentType", "LECTURE");
    form.add("file", new ByteArrayResource(pdf("A process is an executing program. A thread is an execution unit inside a process.")) {
      @Override public String getFilename() { return "operating-systems-notes.pdf"; }
    });
    HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    ResponseEntity<java.util.Map> imported = http.postForEntity(url("/api/v1/documents"), new HttpEntity<>(form, headers), java.util.Map.class);
    assertThat(imported.getStatusCode().value()).isEqualTo(201);
    int documentId = ((Number) ((java.util.Map<?, ?>) imported.getBody().get("data")).get("id")).intValue();

    ResponseEntity<java.util.Map> answered = http.postForEntity(url("/api/v1/qa"),
        java.util.Map.of("projectId", projectId, "question", "What is a process?"), java.util.Map.class);
    assertThat(answered.getStatusCode().is2xxSuccessful()).isTrue();
    java.util.Map<?, ?> answer = (java.util.Map<?, ?>) answered.getBody().get("data");
    assertThat(answer.get("status")).isEqualTo("ANSWERED");
    assertThat((List<?>) answer.get("citations")).isNotEmpty();
    ResponseEntity<Void> deleted = http.exchange(url("/api/v1/documents/" + documentId + "?projectId=" + projectId),
        org.springframework.http.HttpMethod.DELETE, null, Void.class);
    assertThat(deleted.getStatusCode().value()).isEqualTo(204);
  }

  private String url(String path) { return "http://127.0.0.1:" + port + path; }
  private static EmbeddedPostgres startPostgres() {
    try { return EmbeddedPostgres.builder().setPort(0).start(); }
    catch (java.io.IOException error) { throw new IllegalStateException(error); }
  }
  private byte[] pdf(String text) throws Exception {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(); document.addPage(page);
      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12); content.newLineAtOffset(72, 720); content.showText(text); content.endText();
      }
      document.save(output); return output.toByteArray();
    }
  }
}
