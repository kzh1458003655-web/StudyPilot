package cn.studypilot.common.database;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds only connection settings supplied by the local environment.
 *
 * <p>The application never provides a default URL, account, or password. This makes an accidental
 * connection to a shared database impossible on a computer that has not explicitly opted in.</p>
 */
@ConfigurationProperties("studypilot.database")
public record PostgreSqlDatabaseProperties(String url, String username, String password) {}
