package cn.studypilot.common.exception;

/** Raised when an adapter cannot receive a valid response from an external service. */
public final class ExternalServiceException extends BusinessException {
  public ExternalServiceException(ErrorCode code, String message) {
    super(code, message);
    if (code != ErrorCode.EXTERNAL_SERVICE_INVALID_RESPONSE && code != ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE) {
      throw new IllegalArgumentException("ExternalServiceException needs an external-service error code.");
    }
  }
}
