package cn.studypilot.common.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Activates a database-backed component for either formal PostgreSQL or the explicit local demo.
 *
 * <p>Without either setting, the API-only skeleton can still start for contract tests. Keeping this
 * decision in one condition prevents a local demonstration database from silently omitting business
 * controllers that normally rely on {@code studypilot.database.url}.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(StudyPilotDatabaseCondition.class)
public @interface ConditionalOnStudyPilotDatabase {}

final class StudyPilotDatabaseCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String configuredUrl = context.getEnvironment().getProperty("studypilot.database.url");
    boolean localDemo = context.getEnvironment().getProperty(
        "studypilot.local-embedded-db.enabled", Boolean.class, false);
    return StringUtils.hasText(configuredUrl) || localDemo;
  }
}
