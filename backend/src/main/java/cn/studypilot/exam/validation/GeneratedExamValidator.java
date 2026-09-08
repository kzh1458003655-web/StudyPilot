package cn.studypilot.exam.validation;

import cn.studypilot.common.exception.BusinessException;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.exam.model.GeneratedExamItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Rejects malformed model output before it reaches a mock exam or assessment workflow. */
@Component
public class GeneratedExamValidator {
  public List<GeneratedExamItem> validate(List<GeneratedExamItem> candidates, Set<String> allowedSources) {
    if (candidates.isEmpty()) throw invalid("模型没有生成题目");
    Set<String> prompts = new HashSet<>();
    for (GeneratedExamItem item : candidates) {
      if (!Set.of("SINGLE_CHOICE", "SHORT_ANSWER").contains(item.type())) throw invalid("只允许单选题和简答题");
      if (item.prompt() == null || item.prompt().isBlank() || item.knowledgePoint() == null || item.knowledgePoint().isBlank() || item.score() <= 0) throw invalid("题干、知识点和分值必须完整");
      if (!prompts.add(item.prompt().trim())) throw invalid("生成了重复题目");
      if (item.sourceDocumentIds() == null || item.sourceDocumentIds().isEmpty() || !allowedSources.containsAll(item.sourceDocumentIds())) throw invalid("题目缺少当前项目的资料依据");
      if ("SINGLE_CHOICE".equals(item.type())) {
        if (item.options() == null || item.options().size() < 2 || item.options().stream().anyMatch(option -> option == null || option.isBlank())) throw invalid("单选题需要至少两个完整选项");
        if (item.answer() == null || !item.answer().matches("[A-F]")) throw invalid("单选题答案必须是选项编号");
        if (item.answer().charAt(0) - 'A' >= item.options().size()) throw invalid("单选题答案不在选项范围内");
      } else if (item.answer() == null || item.answer().isBlank()) throw invalid("简答题需要参考答案或评分要点");
    }
    return List.copyOf(candidates);
  }

  /** Enforces the fixed, easy-to-review paper shape used by the first product release. */
  public List<GeneratedExamItem> validateMockExam(List<GeneratedExamItem> candidates, Set<String> allowedSources) {
    List<GeneratedExamItem> validated = validate(candidates, allowedSources);
    long choices = validated.stream().filter(item -> "SINGLE_CHOICE".equals(item.type())).count();
    long shortAnswers = validated.stream().filter(item -> "SHORT_ANSWER".equals(item.type())).count();
    if (validated.size() != 4 || choices != 2 || shortAnswers != 2) {
      throw invalid("模拟卷必须包含 2 道单选题和 2 道简答题");
    }
    return validated;
  }
  private BusinessException invalid(String message) { return new BusinessException(ErrorCode.MODEL_OUTPUT_INVALID, message); }
}
