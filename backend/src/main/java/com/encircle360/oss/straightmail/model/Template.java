package com.encircle360.oss.straightmail.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing an email template stored in the database.
 *
 * <p>Only active when the {@code database} Spring profile is enabled.
 * Each template belongs to exactly one tenant (identified by {@code tenantId}) and consists of
 * a FreeMarker HTML body, an optional plain-text body, an optional subject line, an optional locale,
 * and a collection of tags for filtering.
 *
 * <p>Equality is based solely on the {@code id} field to comply with JPA and Hibernate proxy semantics.
 */
@Getter
@Setter
@Entity
@ToString
@SuperBuilder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "templates")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject")
    private String subject;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "html", columnDefinition = "TEXT")
    private String html;

    @Column(name = "plain", columnDefinition = "TEXT")
    private String plain;

    @Column(name = "locale")
    private String locale;

    @Column(name = "tag")
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "template_tags", joinColumns = @JoinColumn(name = "template_id"))
    private List<String> tags;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Template template = (Template) o;
        return getId() != null && Objects.equals(getId(), template.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
