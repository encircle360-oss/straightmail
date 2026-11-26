package com.encircle360.oss.straightmail.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Configuration that enables ShedLock-based distributed scheduling for the Git sync feature.
 *
 * <p>Only active when the {@code database} Spring profile is enabled and
 * {@code templates.git-sync.enabled=true}. Enables Spring's {@code @Scheduled} support and
 * configures a {@link JdbcTemplateLockProvider} backed by the application's data source,
 * ensuring that scheduled Git sync jobs do not run concurrently across multiple application
 * instances (HA deployment). The maximum lock duration defaults to 1 hour (PT1H).
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
@Profile(DatabaseConfig.PROFILE)
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
public class ShedLockConfig {

    @PostConstruct
    void init() {
        log.info("ShedLockConfig loaded — scheduling infrastructure active.");
    }

    /**
     * Creates the ShedLock {@link net.javacrumbs.shedlock.core.LockProvider} backed by JDBC.
     *
     * <p>Uses JVM system time for lock timestamps; this is required because SQLite does not
     * support ShedLock's db-time mode. A custom {@link org.springframework.jdbc.support.SQLExceptionTranslator}
     * maps SQLite constraint violations (error code 19 = {@code SQLITE_CONSTRAINT}) to
     * {@link DuplicateKeyException} so that ShedLock correctly detects a pre-existing lock row
     * instead of propagating the exception as an {@link org.springframework.jdbc.UncategorizedSQLException}.
     *
     * @param dataSource the application's data source
     * @return a configured {@link JdbcTemplateLockProvider}
     */
    @Bean
    public net.javacrumbs.shedlock.core.LockProvider lockProvider(DataSource dataSource) {
        // SQLite returns SQL state null for constraint violations, so Spring cannot map them to
        // DuplicateKeyException automatically. We map error code 19 (SQLITE_CONSTRAINT) manually.
        SQLStateSQLExceptionTranslator fallback = new SQLStateSQLExceptionTranslator();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setExceptionTranslator((task, sql, ex) -> {
            if (ex.getErrorCode() == 19) {
                return new DuplicateKeyException("SQLite constraint violation acquiring ShedLock", ex);
            }
            return fallback.translate(task, sql, ex);
        });
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate)
                        .build());
    }
}
