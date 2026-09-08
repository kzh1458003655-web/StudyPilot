package cn.studypilot.common.exception;

import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** 将模块异常转换为稳定 HTTP 响应；完整业务错误由后续模块扩展。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ErrorResponse> business(BusinessException error, HttpServletRequest request) {
    HttpStatus status = switch (error.code()) {
      case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case MODEL_OUTPUT_INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
      case EXTERNAL_SERVICE_INVALID_RESPONSE -> HttpStatus.BAD_GATEWAY;
      case EXTERNAL_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      default -> HttpStatus.BAD_REQUEST;
    };
    return ResponseEntity.status(status).body(error(error.code(), error.getMessage(), request));
  }
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorResponse> validation(IllegalArgumentException error, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error(ErrorCode.VALIDATION_ERROR, error.getMessage(), request));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> invalidRequest(MethodArgumentNotValidException error, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error(ErrorCode.VALIDATION_ERROR, "请求参数不合法", request));
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception error, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error(ErrorCode.INTERNAL_ERROR, "服务暂时不可用", request));
  }
  private ErrorResponse error(ErrorCode code, String message, HttpServletRequest request) {
    Object id = request.getAttribute(RequestIdFilter.ATTRIBUTE);
    return new ErrorResponse(code.name(), message, id == null ? "unknown" : id.toString(), Instant.now());
  }
}
