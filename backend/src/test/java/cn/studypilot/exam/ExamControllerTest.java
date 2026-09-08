package cn.studypilot.exam;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.exam.controller.ExamController;
import cn.studypilot.exam.model.KnowledgePointFrequency;
import cn.studypilot.exam.model.MockExamDetail;
import cn.studypilot.exam.model.MockExamItemView;
import cn.studypilot.exam.model.SavedMockExam;
import cn.studypilot.exam.service.MockExamGenerationService;
import cn.studypilot.exam.service.MockExamQueryService;
import cn.studypilot.exam.service.PastPaperAnalysisService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies that the exam module follows the project-scoped REST contract. */
@WebMvcTest(controllers = ExamController.class, properties = "studypilot.database.url=jdbc:postgresql://localhost/test")
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class ExamControllerTest {
  @Autowired MockMvc mvc;
  @MockBean PastPaperAnalysisService analysis;
  @MockBean MockExamGenerationService generation;
  @MockBean MockExamQueryService query;

  @Test void generatesTheValidatedMockExamThroughTheCommonEnvelope() throws Exception {
    given(generation.generate(7L)).willReturn(new SavedMockExam(31L, 4));
    mvc.perform(post("/api/v1/exams/generate").param("projectId", "7"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.data.examId").value(31))
        .andExpect(jsonPath("$.data.itemCount").value(4));
  }

  @Test void returnsProjectKnowledgePointFrequency() throws Exception {
    given(analysis.frequencies(anyLong())).willReturn(List.of(new KnowledgePointFrequency("进程与线程", 3)));
    mvc.perform(get("/api/v1/exams/knowledge-points").param("projectId", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].knowledgePoint").value("进程与线程"))
        .andExpect(jsonPath("$.data[0].questionCount").value(3));
  }

  @Test void exposesTheGeneratedPaperWithinItsProject() throws Exception {
    given(query.find(7L, 31L)).willReturn(new MockExamDetail(31L, "模拟卷", List.of(
        new MockExamItemView(1L, 1, "SINGLE_CHOICE", "哪项描述进程？", List.of("程序的一次执行", "静态文件"), "A", "", "进程与线程", 5))));
    mvc.perform(get("/api/v1/exams/31").param("projectId", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("模拟卷"))
        .andExpect(jsonPath("$.data.items[0].options[0]").value("程序的一次执行"));
  }
}
