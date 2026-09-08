package cn.studypilot.common.config;

import cn.studypilot.model.gateway.AiServiceHealth;

/** Browser-safe dependency summary. It contains no connection string, model prompt, or secret. */
public record DependencyHealthResponse(String status, boolean databaseConfigured, AiServiceHealth ai) {}
