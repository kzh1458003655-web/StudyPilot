package cn.studypilot.common.response;

import java.util.List;

/** 所有列表接口统一返回零基页码和稳定的总数。 */
public record PageResponse<T>(List<T> items, int page, int size, long total) {}
