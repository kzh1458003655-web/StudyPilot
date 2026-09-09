package cn.studypilot.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Creates a temporary PostgreSQL instance only for an explicitly requested local demonstration.
 *
 * <p>Formal deployment still requires a PostgreSQL instance and {@code STUDYPILOT_DB_*}
 * environment variables. The temporary database lets a teammate open and test the full web flow
 * before installing PostgreSQL, and its data is not a replacement for formal persisted data.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "studypilot.local-embedded-db", name = "enabled", havingValue = "true")
public class LocalEmbeddedPostgresConfig {
  private static final List<String> SCHEMA_SCRIPTS = List.of(
      "00-schema.sql", "10-common.sql", "20-document.sql", "30-qa.sql", "40-exam.sql",
      "50-assessment.sql", "60-project-archive.sql", "90-indexes.sql");

  /** Starts a private PostgreSQL process on a free local port. */
  @Bean(destroyMethod = "close")
  EmbeddedPostgres localEmbeddedPostgres() throws IOException {
    return EmbeddedPostgres.builder().setPort(0).start();
  }

  /** Uses the formal schema, so local demonstrations exercise the real JDBC repositories. */
  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean(DataSource.class)
  DataSource localEmbeddedDataSource(EmbeddedPostgres postgres) throws Exception {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(postgres.getJdbcUrl("postgres", "postgres"));
    config.setUsername("postgres");
    config.setPassword("local-demo");
    config.setDriverClassName("org.postgresql.Driver");
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    HikariDataSource dataSource = new HikariDataSource(config);
    try (Connection connection = dataSource.getConnection()) {
      for (String script : SCHEMA_SCRIPTS) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/schema/" + script));
      }
    } catch (Exception failure) {
      dataSource.close();
      throw failure;
    }
    return dataSource;
  }

  @Bean
  @ConditionalOnMissingBean(NamedParameterJdbcTemplate.class)
  NamedParameterJdbcTemplate localEmbeddedJdbcTemplate(DataSource localEmbeddedDataSource) {
    return new NamedParameterJdbcTemplate(localEmbeddedDataSource);
  }
}
