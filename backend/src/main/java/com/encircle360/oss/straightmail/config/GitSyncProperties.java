package com.encircle360.oss.straightmail.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Git-sync template provider.
 *
 * <p>Only active when {@code templates.git-sync.enabled=true}.
 */
@Data
@Component
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "templates.git-sync")
public class GitSyncProperties {

    /**
     * Optional base directory for temporary Git clone directories.
     * When set, clones are created as subdirectories of this path instead of the OS temp dir.
     * The directory must exist and be writable. Configurable via {@code GIT_SYNC_BASE_PATH}.
     */
    private String basePath;
}
