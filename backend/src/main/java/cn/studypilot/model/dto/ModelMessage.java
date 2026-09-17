package cn.studypilot.model.dto;

/** 模型协议的最小消息单元。 */
public record ModelMessage(String role, String content) {}
