package cn.studypilot.qa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.qa.controller.QaController;
import cn.studypilot.qa.dto.QaAnswerResponse;
import cn.studypilot.qa.dto.QaCitationResponse;
import cn.studypilot.qa.dto.QaHistoryResponse;
import cn.studypilot.qa.service.GroundedQaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QaController.class, properties = "studypilot.database.url=jdbc:postgresql://localhost/test")
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class QaControllerTest {
  @Autowired MockMvc mvc;
  @MockBean GroundedQaService qa;

  @Test void exposesTheGroundedAnswerUsingTheCommonHttpEnvelope() throws Exception {
    given(qa.ask(any())).willReturn(new QaAnswerResponse(1L, 2L, 3L, "ANSWERED", "进程是程序的一次执行过程。",
        List.of(new QaCitationResponse("讲义.pdf", 4, "进程是程序的一次执行过程。", 0.9))));
    mvc.perform(post("/api/v1/qa").contentType(MediaType.APPLICATION_JSON)
            .content("{\"projectId\":1,\"question\":\"什么是进程？\"}"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.data.answer").value("进程是程序的一次执行过程。"))
        .andExpect(jsonPath("$.data.citations[0].pageNumber").value(4));
  }

  @Test void rejectsBlankQuestionAtTheApiBoundary() throws Exception {
    mvc.perform(post("/api/v1/qa").contentType(MediaType.APPLICATION_JSON)
            .content("{\"projectId\":1,\"question\":\"  \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test void returnsTheLatestSavedConversationForAProject() throws Exception {
    given(qa.history(1L)).willReturn(new QaHistoryResponse(8L, List.of(
        new QaHistoryResponse.QaHistoryTurnResponse("什么是进程？", "进程是程序的一次执行。",
            List.of(new QaCitationResponse("讲义.pdf", 4, "进程是程序的一次执行过程。", .9))))));
    mvc.perform(get("/api/v1/qa/history").param("projectId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(8))
        .andExpect(jsonPath("$.data.turns[0].question").value("什么是进程？"))
        .andExpect(jsonPath("$.data.turns[0].citations[0].pageNumber").value(4));
  }
}
