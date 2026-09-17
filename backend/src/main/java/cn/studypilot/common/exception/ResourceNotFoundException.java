package cn.studypilot.common.exception;

/** 资源归属校验失败时使用，避免以空结果继续执行业务流程。 */
public final class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(String message) { super(ErrorCode.RESOURCE_NOT_FOUND, message); }
}
