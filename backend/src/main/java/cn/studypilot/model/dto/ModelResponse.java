package cn.studypilot.model.dto;

import java.time.Duration;

/** 非流式模型结果与有限运行元数据。 */
public record ModelResponse(String modelName, String content, Duration elapsed) {}
