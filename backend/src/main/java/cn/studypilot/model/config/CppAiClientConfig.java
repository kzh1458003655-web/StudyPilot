package cn.studypilot.model.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Owns the transport setup; business modules only receive Gateway interfaces. */
@Configuration
@EnableConfigurationProperties(CppAiProperties.class)
public class CppAiClientConfig {
  @Bean
  RestClient cppAiRestClient(CppAiProperties properties) {
    return RestClient.builder().baseUrl(properties.baseUrl()).build();
  }
}
