package cn.studypilot.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.document.service.DocumentImportService;
import cn.studypilot.document.service.DocumentIngestionService;
import cn.studypilot.document.service.PdfTextExtractor;
import cn.studypilot.document.service.TextChunker;
import cn.studypilot.retrieval.dto.IndexDocumentRequest;
import cn.studypilot.retrieval.gateway.DocumentIndexGateway;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class DocumentImportServiceTest {
  @Test void persistsBeforePublishingExactPageEvidenceToRetrieval() throws Exception {
    DocumentRepository repository = Mockito.mock(DocumentRepository.class);
    DocumentIndexGateway gateway = Mockito.mock(DocumentIndexGateway.class);
    given(repository.create(eq(7L), any(), eq("讲义.pdf"), eq("LECTURE"), anyString(), anyString())).willReturn(31L);
    var service = new DocumentImportService(new DocumentIngestionService(new PdfTextExtractor(), new TextChunker()), repository, gateway);
    var imported = service.importPdf(7, "讲义.pdf", "LECTURE", "files/lecture.pdf", "hash", new ByteArrayInputStream(pdf("retrieval evidence")));
    assertThat(imported.documentId()).isEqualTo(31L);
    verify(repository).replaceExtractedContent(eq(31L), anyList(), anyList());
    ArgumentCaptor<IndexDocumentRequest> payload = ArgumentCaptor.forClass(IndexDocumentRequest.class);
    verify(gateway).index(payload.capture());
    verify(repository).markReady(31L);
    assertThat(payload.getValue().pages().getFirst().text()).contains("retrieval evidence");

    InOrder order = Mockito.inOrder(repository, gateway);
    order.verify(repository).replaceExtractedContent(eq(31L), anyList(), anyList());
    order.verify(gateway).index(any());
    order.verify(repository).markReady(31L);
  }

  @Test void marksDocumentFailedWhenRetrievalIndexingDoesNotComplete() throws Exception {
    DocumentRepository repository = Mockito.mock(DocumentRepository.class);
    DocumentIndexGateway gateway = Mockito.mock(DocumentIndexGateway.class);
    given(repository.create(eq(7L), any(), anyString(), anyString(), anyString(), anyString())).willReturn(32L);
    doThrow(new IllegalStateException("C++ 索引服务不可用")).when(gateway).index(any());

    var service = new DocumentImportService(new DocumentIngestionService(new PdfTextExtractor(), new TextChunker()), repository, gateway);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importPdf(7, "讲义.pdf", "LECTURE", "files/lecture.pdf", "hash", new ByteArrayInputStream(pdf("failure"))))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("索引服务不可用");
    verify(repository).markFailed(32L, "C++ 索引服务不可用");
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).markReady(32L);
  }

  private byte[] pdf(String text) throws Exception {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(); document.addPage(page);
      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(72, 720); content.showText(text); content.endText();
      }
      document.save(output); return output.toByteArray();
    }
  }
}
