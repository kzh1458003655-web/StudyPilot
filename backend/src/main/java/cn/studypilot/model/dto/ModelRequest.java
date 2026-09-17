package cn.studypilot.model.dto;

import java.time.Duration;
import java.util.List;

/** Gateway 只接收已完成业务编排的消息，不知道资料、试卷或作答实体。 */
public record ModelRequest(ModelTaskType taskType, List<ModelMessage> messages, double temperature, int maxTokens,
                           Duration timeout) {}
