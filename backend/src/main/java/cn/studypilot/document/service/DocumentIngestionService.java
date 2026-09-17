package cn.studypilot.document.service;

import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.ExtractedPage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Service;

/** Pure ingestion core; persistence and C++ index publication are orchestrated by the module workflow. */
@Service
public class DocumentIngestionService {
  private final PdfTextExtractor extractor;
  private final TextChunker chunker;

  public DocumentIngestionService(PdfTextExtractor extractor, TextChunker chunker) {
    this.extractor = extractor;
    this.chunker = chunker;
  }

  public IngestionResult ingest(InputStream source) throws IOException {
    List<ExtractedPage> pages = extractor.extract(source);
    List<DocumentChunk> chunks = chunker.chunk(pages);
    return new IngestionResult(pages, chunks);
  }

  public record IngestionResult(List<ExtractedPage> pages, List<DocumentChunk> chunks) {}
}
