package cn.studypilot.common.exception;

/** Raised when a model response fails the calling module's explicit rules. */
public final class ModelOutputValidationException extends BusinessException {
  public ModelOutputValidationException(String message) {
    super(ErrorCode.MODEL_OUTPUT_INVALID, message);
  }
}
