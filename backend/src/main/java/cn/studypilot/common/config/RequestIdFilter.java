package cn.studypilot.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为每个请求生成可用于日志和错误响应的追踪号。 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
  public static final String ATTRIBUTE = "studyPilotRequestId";
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String id = UUID.randomUUID().toString(); request.setAttribute(ATTRIBUTE, id); response.setHeader("X-Request-Id", id);
    chain.doFilter(request, response);
  }
}
