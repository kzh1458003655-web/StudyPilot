package cn.studypilot.exam.algorithm;

import cn.studypilot.document.model.ExtractedPage;
import cn.studypilot.exam.model.ParsedSourceQuestion;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Splits text-layer past papers using visible numbered-question markers.
 *
 * <p>This intentionally produces no result for an unnumbered or image-dependent page. The caller
 * can report it as requiring confirmation instead of inventing a question boundary.</p>
 */
@Component
public class PastPaperQuestionParser {
  private static final Pattern LABELED_MARKER = Pattern.compile(
      "(?im)^\\s*(?:problem|question|题目|第)\\s*(\\d{1,3})\\s*[.．、:：)]?\\s*");
  private static final Pattern PAREN_MARKER = Pattern.compile("(?m)^\\s*\\(\\s*(\\d{1,3})\\s*\\)\\s*");
  private static final Pattern DOT_MARKER = Pattern.compile("(?m)^\\s*(?:第\\s*)?(\\d{1,3})\\s*[.．、)]\\s*");

  public List<ParsedSourceQuestion> parse(List<ExtractedPage> pages) {
    if (pages.isEmpty()) return List.of();
    StringBuilder combined = new StringBuilder();
    List<Integer> pageOffsets = new ArrayList<>();
    for (ExtractedPage page : pages) {
      pageOffsets.add(combined.length());
      combined.append(page.text()).append('\n');
    }
    String text = combined.toString();
    // Many English papers use “Problem 1.” and many Chinese/OCW papers use “(1)”.
    // Prefer those explicit forms so numbered answer choices are not mistaken for questions.
    List<ParsedSourceQuestion> labeled = split(text, pages, pageOffsets, LABELED_MARKER);
    if (!labeled.isEmpty()) return labeled;
    List<ParsedSourceQuestion> parenthesized = split(text, pages, pageOffsets, PAREN_MARKER);
    if (!parenthesized.isEmpty()) return parenthesized;
    // Fallback for simple text exports such as “1. … / 2、 …”.
    return split(text, pages, pageOffsets, DOT_MARKER);
  }

  private List<ParsedSourceQuestion> split(String text, List<ExtractedPage> pages,
      List<Integer> pageOffsets, Pattern pattern) {
    List<ParsedSourceQuestion> parsed = new ArrayList<>();
    Matcher marker = pattern.matcher(text);
    List<Integer> starts = new ArrayList<>();
    List<Integer> ordinals = new ArrayList<>();
    while (marker.find()) {
      starts.add(marker.start());
      ordinals.add(Integer.parseInt(marker.group(1)));
    }
    for (int index = 0; index < starts.size(); index++) {
      int end = index + 1 < starts.size() ? starts.get(index + 1) : text.length();
      String question = text.substring(starts.get(index), end).trim();
      if (question.length() >= 8) {
        parsed.add(new ParsedSourceQuestion(ordinals.get(index), pageAt(starts.get(index), pages, pageOffsets), question));
      }
    }
    return List.copyOf(parsed);
  }

  private int pageAt(int offset, List<ExtractedPage> pages, List<Integer> pageOffsets) {
    int pageIndex = 0;
    for (int index = 1; index < pageOffsets.size(); index++) {
      if (pageOffsets.get(index) > offset) break;
      pageIndex = index;
    }
    return pages.get(pageIndex).pageNumber();
  }
}
