package cn.studypilot;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ErrorHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> error(Exception e) {
    int status = e instanceof NoSuchElementException ? 404
        : e instanceof IllegalArgumentException      ? 400
        : e instanceof IllegalStateException         ? 409
                                                     : 503;
    String message = (status == 400 || status == 404 || status == 409)
        ? e.getMessage()
        : "服务暂不可用，请检查文件内容或本地服务状态";
    return ResponseEntity.status(status).body(
        Map.of("message", Objects.requireNonNullElse(message, "请求失败")));
  }
}
