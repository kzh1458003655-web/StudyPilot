package cn.studypilot.document.service;

import cn.studypilot.document.model.ExtractedPage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

/** Extracts the text layer page by page. Scanned PDFs deliberately fail rather than pretending OCR exists. */
@Service
public class PdfTextExtractor {
  public List<ExtractedPage> extract(InputStream source) throws IOException {
    try (PDDocument document = Loader.loadPDF(source.readAllBytes())) {
      PDFTextStripper stripper = new PDFTextStripper();
      List<ExtractedPage> pages = new ArrayList<>();
      for (int page = 1; page <= document.getNumberOfPages(); page++) {
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        String text = stripper.getText(document).replaceAll("\\s+", " ").trim();
        if (!text.isBlank()) pages.add(new ExtractedPage(page, text));
      }
      if (pages.isEmpty()) throw new IllegalArgumentException("PDF 未包含可提取的文字内容");
      return List.copyOf(pages);
    }
  }
}
