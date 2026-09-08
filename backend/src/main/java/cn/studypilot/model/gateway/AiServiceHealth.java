package cn.studypilot.model.gateway;

/** Stable Java view of the C++ service health payload; browser clients never call the C++ port. */
public record AiServiceHealth(
    String service, boolean modelReady, int chunks, int pending, long completed) {}
