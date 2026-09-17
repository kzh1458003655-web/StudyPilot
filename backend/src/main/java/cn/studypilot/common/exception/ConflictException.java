package cn.studypilot.common.exception;

/** Raised when a request conflicts with an existing resource or state. */
public final class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super(ErrorCode.CONFLICT, message);
  }
}
