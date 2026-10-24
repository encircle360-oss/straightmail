package com.encircle360.oss.straightmail.repository;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.GitSyncStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link GitSyncStatus} entities.
 *
 * <p>Only active when the {@code database} Spring profile is enabled.
 */
@Repository
@Profile(DatabaseConfig.PROFILE)
public interface GitSyncStatusRepository extends JpaRepository<GitSyncStatus, String> {

    List<GitSyncStatus> findAllByOrderByTenantIdAsc();
}
