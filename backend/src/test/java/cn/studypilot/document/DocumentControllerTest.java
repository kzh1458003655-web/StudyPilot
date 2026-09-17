package cn.studypilot.document;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.document.controller.DocumentController;
import cn.studypilot.document.service.DocumentImportService;
import cn.studypilot.document.service.LocalDocumentStorage;
import cn.studypilot.document.service.DocumentResourceService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Checks the multipart boundary and the public response; PDF parsing is covered by service tests. */
@WebMvcTest(controllers = DocumentController.class, properties = "studypilot.database.url=jdbc:postgresql://localhost/test")
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class DocumentControllerTest {
  @Autowired MockMvc mvc;
  @MockBean LocalDocumentStorage storage;
  @MockBean DocumentImportService importer;
  @MockBean DocumentResourceService resources;

  @Test void uploadsPdfAndReturnsTheImportSummary() throws Exception {
    Path stored = Files.createTempFile("studypilot-document-", ".pdf");
    try {
      given(storage.store(eq(8L), any())).willReturn(new LocalDocumentStorage.StoredFile(stored.toString(), "abc", "讲义.pdf"));
      given(importer.importPdf(eq(8L), eq("讲义.pdf"), eq("LECTURE"), eq(stored.toString()), eq("abc"), any()))
          .willReturn(new DocumentImportService.ImportedDocument(19L, "index-19", 3, 7));

      mvc.perform(multipart("/api/v1/documents")
              .file(new MockMultipartFile("file", "讲义.pdf", "application/pdf", "pdf-content".getBytes()))
              .param("projectId", "8").param("documentType", "LECTURE"))
          .andExpect(status().isCreated())
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.data.id").value(19))
          .andExpect(jsonPath("$.data.pageCount").value(3))
          .andExpect(jsonPath("$.requestId").isNotEmpty());
    } finally {
      Files.deleteIfExists(stored);
    }
  }

  @Test void rejectsAnUnsupportedDocumentTypeBeforeItWritesAFile() throws Exception {
    mvc.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file", "讲义.pdf", "application/pdf", "pdf-content".getBytes()))
            .param("projectId", "8").param("documentType", "OTHER"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
