package cn.studypilot.common.config;

import cn.studypilot.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 骨架健康接口不探测数据库或外部服务，保证无外部依赖时可用于基础验证。 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
  @GetMapping public ApiResponse<Map<String, String>> health(HttpServletRequest request) {
    return new ApiResponse<>(Map.of("status", "UP"), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
}
