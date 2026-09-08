package cn.studypilot.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * Creates the PostgreSQL JDBC boundary only when the operator explicitly supplies a JDBC URL.
 *
 * <p>Keeping the condition here is deliberate: architecture and Gateway tests can run without a
 * database, while a deployed instance uses the same {@link NamedParameterJdbcTemplate} as every
 * module repository. No JPA entity scanning or runtime DDL is enabled.</p>
 */
@Configuration
@EnableConfigurationProperties(PostgreSqlDatabaseProperties.class)
@ConditionalOnProperty(prefix = "studypilot.database", name = "url")
public class PostgreSqlJdbcConfig {
  @Bean(destroyMethod = "close")
  DataSource studyPilotDataSource(PostgreSqlDatabaseProperties properties) {
    if (!StringUtils.hasText(properties.url()) || !properties.url().startsWith("jdbc:postgresql:")) {
      throw new IllegalStateException("STUDYPILOT_DB_URL must be a jdbc:postgresql URL.");
    }
    if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
      throw new IllegalStateException("STUDYPILOT_DB_USERNAME and STUDYPILOT_DB_PASSWORD are required when a database URL is set.");
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.url());
    config.setUsername(properties.username());
    config.setPassword(properties.password());
    config.setDriverClassName("org.postgresql.Driver");
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(10_000);
    // A connection is verified on the first repository operation, not while running API-only tests.
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  @Bean
  NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource studyPilotDataSource) {
    return new NamedParameterJdbcTemplate(studyPilotDataSource);
  }
}
