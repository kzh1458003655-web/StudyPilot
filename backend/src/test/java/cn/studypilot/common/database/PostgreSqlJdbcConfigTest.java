package cn.studypilot.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Verifies that database access is opt-in and exposes only Spring JDBC primitives. */
class PostgreSqlJdbcConfigTest {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(PostgreSqlJdbcConfig.class);

  @Test void doesNotCreateDataSourceWithoutExplicitConnectionSettings() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
  }

  @Test void createsNamedJdbcBoundaryWhenLocalSettingsAreProvided() {
    contextRunner
        .withPropertyValues(
            "studypilot.database.url=jdbc:postgresql://127.0.0.1:5432/studypilot_test",
            "studypilot.database.username=tester",
            "studypilot.database.password=tester")
        .run(context -> {
          assertThat(context).hasSingleBean(NamedParameterJdbcTemplate.class);
          assertThat(context).getBean(DataSource.class).isInstanceOf(HikariDataSource.class);
        });
  }
}
