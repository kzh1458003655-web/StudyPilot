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
  private static final Pattern MARKER = Pattern.compile("(?m)^\\s*(?:第\\s*)?(\\d{1,3})\\s*[.．、)]\\s*");

  public List<ParsedSourceQuestion> parse(List<ExtractedPage> pages) {
    List<ParsedSourceQuestion> parsed = new ArrayList<>();
    for (ExtractedPage page : pages) {
      Matcher marker = MARKER.matcher(page.text());
      List<Integer> starts = new ArrayList<>();
      List<Integer> ordinals = new ArrayList<>();
      while (marker.find()) { starts.add(marker.start()); ordinals.add(Integer.parseInt(marker.group(1))); }
      for (int index = 0; index < starts.size(); index++) {
        int end = index + 1 < starts.size() ? starts.get(index + 1) : page.text().length();
        String text = page.text().substring(starts.get(index), end).trim();
        if (text.length() >= 8) parsed.add(new ParsedSourceQuestion(ordinals.get(index), page.pageNumber(), text));
      }
    }
    return List.copyOf(parsed);
  }
}
