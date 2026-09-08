package cn.studypilot.common.config;

import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.model.gateway.AiServiceHealthGateway;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Architecture diagnostic endpoint. The global handler returns 503 when C++ is unavailable. */
@RestController
@RequestMapping("/api/v1/health/dependencies")
public class DependencyHealthController {
  private final AiServiceHealthGateway aiHealth;
  private final ObjectProvider<DataSource> dataSource;

  public DependencyHealthController(AiServiceHealthGateway aiHealth, ObjectProvider<DataSource> dataSource) {
    this.aiHealth = aiHealth;
    this.dataSource = dataSource;
  }

  @GetMapping public ApiResponse<DependencyHealthResponse> dependencies(HttpServletRequest request) {
    var response = new DependencyHealthResponse("UP", dataSource.getIfAvailable() != null, aiHealth.health());
    return new ApiResponse<>(response, request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
