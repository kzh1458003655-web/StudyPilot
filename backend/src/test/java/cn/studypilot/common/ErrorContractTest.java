package cn.studypilot.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Confirms that a module exception is exposed through the public error contract. */
@WebMvcTest(controllers = MissingResourceEndpoint.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class ErrorContractTest {
  @Autowired MockMvc mvc;

  @Test void missingResourceUsesStableNotFoundContract() throws Exception {
    mvc.perform(get("/test/missing"))
        .andExpect(status().isNotFound())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }
}
