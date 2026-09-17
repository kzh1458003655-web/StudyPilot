package cn.studypilot.common;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.DependencyHealthController;
import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.common.exception.ExternalServiceException;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.model.gateway.AiServiceHealth;
import cn.studypilot.model.gateway.AiServiceHealthGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for the successful and unavailable internal-AI dependency paths. */
@WebMvcTest(controllers = DependencyHealthController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class DependencyHealthControllerTest {
  @Autowired MockMvc mvc;
  @MockBean AiServiceHealthGateway aiHealth;

  @Test void reportsCppStatusThroughThePublicApiContract() throws Exception {
    given(aiHealth.health()).willReturn(new AiServiceHealth("StudyPilot C++", true, 47, 0, 1));
    mvc.perform(get("/api/v1/health/dependencies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.ai.modelReady").value(true))
        .andExpect(jsonPath("$.data.ai.chunks").value(47));
  }

  @Test void returns503WhenCppIsUnavailable() throws Exception {
    given(aiHealth.health()).willThrow(new ExternalServiceException(
        ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "AI 服务不可用或响应超时"));
    mvc.perform(get("/api/v1/health/dependencies"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
  }
}
