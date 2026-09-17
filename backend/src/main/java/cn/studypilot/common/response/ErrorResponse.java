package cn.studypilot.common.response;

import java.time.Instant;

/** 对外错误格式不暴露堆栈、SQL、密钥或外部服务内部信息。 */
public record ErrorResponse(String code, String message, String requestId, Instant timestamp) {}
