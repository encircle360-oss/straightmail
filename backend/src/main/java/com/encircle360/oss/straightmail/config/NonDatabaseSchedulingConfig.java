package com.encircle360.oss.straightmail.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} support when Git-sync is active.
 *
 * <p>When the {@code database} profile is also active, {@link ShedLockConfig} independently
 * provides {@code @EnableScheduling} and the ShedLock distributed-lock infrastructure.
 * Having two {@code @EnableScheduling} declarations in the same context is harmless.
 * Without the {@code database} profile, this configuration is the sole source of scheduling support.
 * In that case, {@code @SchedulerLock} annotations on scheduled methods are silently ignored
 * because no {@code @EnableSchedulerLock} aspect is registered.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
public class NonDatabaseSchedulingConfig {
}
