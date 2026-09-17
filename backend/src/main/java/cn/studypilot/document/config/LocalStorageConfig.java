package cn.studypilot.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables storage configuration without coupling the file service to environment variables. */
@Configuration
@EnableConfigurationProperties(LocalStorageProperties.class)
public class LocalStorageConfig {
}
