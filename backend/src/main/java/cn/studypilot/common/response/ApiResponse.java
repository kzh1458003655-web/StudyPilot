package cn.studypilot.common.response;

/** 普通 REST 接口的统一成功包装；SSE 事件不使用此结构。 */
public record ApiResponse<T>(T data, String requestId) {}
