package com.encircle360.oss.straightmail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring configuration that activates JPA repository support for the {@code database} profile.
 *
 * <p>When the {@code database} Spring profile is active, this class enables JPA repositories
 * under {@code com.encircle360.oss.straightmail.repository}. Without this profile, no database
 * connectivity or JPA entities are loaded, and the application operates in file-only template mode.
 *
 * <p>{@link #PROFILE} is a shared constant referenced by all profile-specific components to avoid
 * magic string duplication.
 */
@Configuration
@Profile(DatabaseConfig.PROFILE)
@EnableJpaRepositories("com.encircle360.oss.straightmail.repository")
public class DatabaseConfig {

    /**
     * The Spring profile name that activates database mode ({@value}).
     */
    public static final String PROFILE = "database";
}
