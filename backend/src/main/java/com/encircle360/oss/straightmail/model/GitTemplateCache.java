package com.encircle360.oss.straightmail.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity caching a single FreeMarker template that was cloned from a Git repository.
 *
 * <p>Only written and read when the {@code database} Spring profile is active.
 * The entity class itself carries no profile restriction so that Hibernate can scan it in
 * all configurations without errors — only the repository and service layer are profile-gated
 * (same pattern as {@link Template}).
 *
 * <p>The natural key is {@code (tenantId, branch, templateName)}, enforced by a unique constraint.
 * A surrogate UUID primary key is used for compatibility with standard Spring Data JPA repositories.
 *
 * <p>{@code branchOrder} reflects the position of this branch in the tenant's configured
 * {@code gitBranches} list, enabling deterministic priority-ordered resolution across branches.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(
        name = "git_template_cache",
        indexes = @Index(name = "uq_git_template_cache", columnList = "tenant_id, branch, template_name", unique = true)
)
public class GitTemplateCache {

    @Id
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "branch_order", nullable = false)
    private int branchOrder;

    @Column(name = "template_name", nullable = false, length = 512)
    private String templateName;

    @Column(name = "html", columnDefinition = "TEXT")
    private String html;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "plain", columnDefinition = "TEXT")
    private String plain;
}
