package cn.studypilot.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.exception.GlobalExceptionHandler;
import cn.studypilot.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Confirms that a module exception is exposed through the public error contract. */
@WebMvcTest(controllers = MissingResourceEndpoint.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
@EnableConfigurationProperties(MultipartProperties.class)
class ErrorContractTest {
  @Autowired MockMvc mvc;
  @Autowired MultipartProperties multipartProperties;

  @Test
  void missingResourceUsesStableNotFoundContract() throws Exception {
    mvc.perform(get("/test/missing"))
        .andExpect(status().isNotFound())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void oversizedUploadUsesReadablePayloadTooLargeContract() throws Exception {
    mvc.perform(get("/test/oversized-upload"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("单个 PDF 文件不得超过 25 MB"));
  }

  @Test
  void configAllowsTypicalCoursePdfFilesUpToTwentyFiveMegabytes() {
    assertThat(multipartProperties.getMaxFileSize().toBytes()).isEqualTo(25L * 1024 * 1024);
    assertThat(multipartProperties.getMaxRequestSize().toBytes()).isEqualTo(26L * 1024 * 1024);
  }
}
