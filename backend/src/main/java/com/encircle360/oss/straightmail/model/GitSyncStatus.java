package com.encircle360.oss.straightmail.model;

import com.encircle360.oss.straightmail.dto.status.SyncResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * JPA entity persisting the most recent Git-sync outcome per tenant.
 *
 * <p>Only written and read when the {@code database} Spring profile is active.
 * The entity class itself carries no profile restriction so that Hibernate can scan it in
 * all configurations without errors — only the repository and service layer are profile-gated
 * (same pattern as {@link Template}).
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "git_sync_status")
public class GitSyncStatus {

    @Id
    @Column(name = "tenant_id", length = 63)
    private String tenantId;

    // SQLite JDBC does not support getTimestamp() on integer columns; force BIGINT so
    // Hibernate uses getLong() (epoch millis) instead of the broken getTimestamp() path.
    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 16)
    private SyncResult result;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;
}
