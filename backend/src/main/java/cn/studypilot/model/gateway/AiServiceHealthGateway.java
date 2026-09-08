package cn.studypilot.model.gateway;

/** Probes the separately managed C++ service without exposing its port to the browser. */
public interface AiServiceHealthGateway {
  AiServiceHealth health();
}
