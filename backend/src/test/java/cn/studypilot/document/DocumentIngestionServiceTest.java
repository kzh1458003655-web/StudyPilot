package cn.studypilot.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.studypilot.document.service.DocumentIngestionService;
import cn.studypilot.document.service.PdfTextExtractor;
import cn.studypilot.document.service.TextChunker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class DocumentIngestionServiceTest {
  private final DocumentIngestionService service = new DocumentIngestionService(new PdfTextExtractor(), new TextChunker());

  @Test void keepsPageNumbersAndProducesRetrievalChunks() throws Exception {
    var result = service.ingest(new ByteArrayInputStream(createPdf("first page evidence", "second page evidence")));
    assertThat(result.pages()).extracting(page -> page.pageNumber()).containsExactly(1, 2);
    assertThat(result.chunks()).extracting(chunk -> chunk.pageNumber()).containsExactly(1, 2);
    assertThat(result.chunks().getFirst().text()).contains("first page evidence");
  }

  @Test void rejectsPdfWithoutTextLayer() throws Exception {
    assertThatThrownBy(() -> service.ingest(new ByteArrayInputStream(createPdf("", ""))))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("可提取");
  }

  private byte[] createPdf(String first, String second) throws Exception {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      appendPage(document, first);
      appendPage(document, second);
      document.save(output);
      return output.toByteArray();
    }
  }

  private void appendPage(PDDocument document, String text) throws Exception {
    PDPage page = new PDPage();
    document.addPage(page);
    if (!text.isBlank()) try (PDPageContentStream content = new PDPageContentStream(document, page)) {
      content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
      content.newLineAtOffset(72, 720); content.showText(text); content.endText();
    }
  }
}
