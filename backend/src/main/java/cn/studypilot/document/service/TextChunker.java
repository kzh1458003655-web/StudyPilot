package cn.studypilot.document.service;

import cn.studypilot.document.model.DocumentChunk;
import cn.studypilot.document.model.ExtractedPage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Deterministic page-local chunker. The overlap keeps an answer at a boundary retrievable. */
@Service
public class TextChunker {
  static final int MAX_CHARACTERS = 800;
  static final int OVERLAP_CHARACTERS = 120;

  public List<DocumentChunk> chunk(List<ExtractedPage> pages) {
    List<DocumentChunk> chunks = new ArrayList<>();
    int index = 0;
    for (ExtractedPage page : pages) {
      String text = page.text();
      for (int start = 0; start < text.length();) {
        int end = Math.min(text.length(), start + MAX_CHARACTERS);
        if (end < text.length()) {
          int boundary = text.lastIndexOf(' ', end);
          if (boundary > start + MAX_CHARACTERS / 2) end = boundary;
        }
        String chunk = text.substring(start, end).trim();
        if (!chunk.isBlank()) chunks.add(new DocumentChunk(page.pageNumber(), index++, chunk, estimateTokens(chunk)));
        if (end >= text.length()) break;
        start = Math.max(end - OVERLAP_CHARACTERS, start + 1);
      }
    }
    return List.copyOf(chunks);
  }

  private int estimateTokens(String text) {
    return Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 3.0));
  }
}
