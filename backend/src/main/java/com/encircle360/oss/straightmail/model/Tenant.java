package com.encircle360.oss.straightmail.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity representing a tenant in the multi-tenancy setup.
 *
 * <p>Only active when the {@code database} Spring profile is enabled.
 * The slug serves as the primary key and must be unique across all tenants.
 *
 * <p>Sensitive fields are stored encrypted ({@code smtpPassword}, {@code gitToken}) or
 * as a SHA-256 hash ({@code apiKeyHash}). They are never returned in API responses.
 *
 * <p>Equality is based solely on the {@code slug} field to comply with JPA and Hibernate proxy semantics.
 */
@Getter
@Setter
@Entity
@Builder
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "tenants")
public class Tenant {

    /**
     * Slug identifier used for the built-in default tenant that is always present.
     */
    public static final String DEFAULT_ID = "default";

    @Id
    @Column(name = "slug", length = 63)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Column(name = "brand_color", length = 7)
    private String brandColor;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_user")
    private String smtpUser;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "smtp_sender")
    private String smtpSender;

    @Column(name = "smtp_tls")
    private boolean smtpTls;

    @Column(name = "smtp_ssl")
    private boolean smtpSsl;

    @Column(name = "git_token")
    private String gitToken;

    @Column(name = "git_repo_url")
    private String gitRepoUrl;

    @Builder.Default
    @Column(name = "branch")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_git_branches", joinColumns = @JoinColumn(name = "tenant_slug"))
    private List<String> gitBranches = new ArrayList<>();

    @Column(name = "api_key_hash", length = 64)
    private String apiKeyHash;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Tenant tenant = (Tenant) o;
        return getSlug() != null && Objects.equals(getSlug(), tenant.getSlug());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
