package cn.studypilot.exam;

import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.document.model.ExtractedPage;
import cn.studypilot.exam.algorithm.KnowledgePointNormalizer;
import cn.studypilot.exam.algorithm.PastPaperQuestionParser;
import org.junit.jupiter.api.Test;

class PastPaperAnalysisAlgorithmTest {
  @Test void splitsOnlyExplicitTextQuestionsAndKeepsThePageBoundary() {
    var questions = new PastPaperQuestionParser().parse(java.util.List.of(new ExtractedPage(3, "1. 什么是进程？\n2、说明线程与进程的区别。")));
    assertThat(questions).extracting("ordinal").containsExactly(1, 2);
    assertThat(questions).extracting("pageNumber").containsOnly(3);
    assertThat(new PastPaperQuestionParser().parse(java.util.List.of(new ExtractedPage(4, "这是一页没有题号的扫描转写")))).isEmpty();
  }

  @Test void normalizesSynonymsOncePerQuestion() {
    assertThat(new KnowledgePointNormalizer().extractAndNormalize("请比较 process 与 thread，并说明线程调度。"))
        .containsExactlyInAnyOrder("进程与线程", "进程调度");
  }
}
