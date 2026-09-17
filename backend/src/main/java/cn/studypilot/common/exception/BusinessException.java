package cn.studypilot.common.exception;

/** 可预期业务失败的基类，避免 Controller 通过字符串判断错误类型。 */
public class BusinessException extends RuntimeException {
  private final ErrorCode code;
  public BusinessException(ErrorCode code, String message) { super(message); this.code = code; }
  public ErrorCode code() { return code; }
}
