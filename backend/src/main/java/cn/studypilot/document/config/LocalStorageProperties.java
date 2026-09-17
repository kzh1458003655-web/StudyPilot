package cn.studypilot.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurable root for local user-generated files. */
@ConfigurationProperties("studypilot.storage")
public record LocalStorageProperties(String root) {
}
