package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.studypilot.exam.model.GeneratedExamItem;
import cn.studypilot.exam.validation.GeneratedExamValidator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeneratedExamValidatorTest {
  @Test void acceptsGroundedSingleChoiceAndShortAnswerOnly() {
    var valid = List.of(new GeneratedExamItem("SINGLE_CHOICE", "哪项描述进程？", List.of("A", "B"), "A", "", "进程与线程", 5, List.of("doc-1")),
        new GeneratedExamItem("SHORT_ANSWER", "解释死锁。", List.of(), "互相等待资源。", "", "死锁", 10, List.of("doc-1")));
    assertThat(new GeneratedExamValidator().validate(valid, Set.of("doc-1"))).hasSize(2);
  }
  @Test void rejectsUngroundedOrMalformedOutput() {
    var invalid = List.of(new GeneratedExamItem("MULTI_CHOICE", "题目", List.of("A", "B"), "A", "", "知识点", 1, List.of("other")));
    assertThatThrownBy(() -> new GeneratedExamValidator().validate(invalid, Set.of("doc-1")))
        .hasMessageContaining("只允许单选题和简答题");
  }
  @Test void mockExamMustUseTheFixedTwoPlusTwoQuestionShape() {
    var tooShort = List.of(new GeneratedExamItem("SINGLE_CHOICE", "哪项描述进程？", List.of("A", "B"), "A", "", "进程与线程", 5, List.of("doc-1")));
    assertThatThrownBy(() -> new GeneratedExamValidator().validateMockExam(tooShort, Set.of("doc-1")))
        .hasMessageContaining("2 道单选题和 2 道简答题");
  }
}
