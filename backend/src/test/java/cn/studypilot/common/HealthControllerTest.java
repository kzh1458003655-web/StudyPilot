package cn.studypilot.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.HealthController;
import cn.studypilot.common.config.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** 骨架必须在数据库、C++ 检索和本地模型均未启动时完成 HTTP 基础验证。 */
@WebMvcTest(controllers = HealthController.class)
@Import(RequestIdFilter.class)
class HealthControllerTest {
  @Autowired MockMvc mvc;
  @Test void healthUsesVersionedPathAndRequestId() throws Exception {
    mvc.perform(get("/api/v1/health"))
        .andExpect(status().isOk()).andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.data.status").value("UP")).andExpect(jsonPath("$.requestId").isNotEmpty());
  }
}
