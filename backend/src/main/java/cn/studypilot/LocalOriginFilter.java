package cn.studypilot;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
/**
 * 本地部署的最小同源边界。
 *
 * <p>桌面浏览器只能从本机同端口访问 Java 服务；C++ 模型端口还会拒绝任何 Origin。
 * 它不是登录鉴权的替代品，但能防止其他网页直接向本机服务发出带副作用的请求。
 */
public class LocalOriginFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String origin = request.getHeader("Origin");
    if (origin != null) {
      // 没有 Origin 的请求可能来自启动脚本和健康检查；携带 Origin 时则必须严格同源。
      boolean allowed = false;
      try {
        URI uri = URI.create(origin);
        allowed = ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()))
            && uri.getPort() == request.getServerPort() && "http".equals(uri.getScheme());
      } catch (Exception ignored) {
      }
      if (!allowed) {
        response.sendError(403);
        return;
      }
    }
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    chain.doFilter(request, response);
  }
}
