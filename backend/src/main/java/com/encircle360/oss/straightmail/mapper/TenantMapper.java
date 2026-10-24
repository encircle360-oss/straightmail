package com.encircle360.oss.straightmail.mapper;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.dto.tenant.TenantDTO;
import com.encircle360.oss.straightmail.model.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between {@link Tenant} entities and their DTO representations.
 *
 * <p>Handles the following mapping directions:
 * <ul>
 *   <li>{@link Tenant} → {@link TenantDTO} (read response, sensitive fields omitted)</li>
 *   <li>{@link TenantProperties.TenantConfig} → {@link TenantDTO} (non-database mode, read response)</li>
 *   <li>{@link CreateUpdateTenantDTO} → {@link Tenant} (create, sensitive fields excluded for separate handling)</li>
 *   <li>{@link CreateUpdateTenantDTO} → existing {@link Tenant} (update in-place)</li>
 *   <li>{@link TenantProperties.TenantConfig} → {@link Tenant} (reconciliation create/update)</li>
 * </ul>
 *
 * <p>Sensitive credentials ({@code smtpPassword}, {@code gitToken}, {@code apiKeyHash}) are always
 * excluded from mappings and handled separately by {@link com.encircle360.oss.straightmail.service.TenantService}.
 */
@Mapper(componentModel = "spring")
public interface TenantMapper {

    /**
     * Maps a {@link Tenant} entity to a {@link TenantDTO}.
     * The {@code hasApiKey} field is derived from the presence of a non-null {@code apiKeyHash}.
     *
     * @param tenant the tenant entity
     * @return the mapped DTO (without sensitive fields)
     */
    @Mapping(target = "hasApiKey", expression = "java(tenant.getApiKeyHash() != null)")
    @Mapping(target = "editable", expression = "java(true)")
    TenantDTO toDto(Tenant tenant);

    /**
     * Maps a static {@link TenantProperties.TenantConfig} entry to a {@link TenantDTO}.
     * Used when serving tenant data in non-database mode.
     * {@code hasApiKey} is derived from the presence of a non-blank {@code apiKey} in the config;
     * {@code active} defaults to {@code true} when not explicitly set.
     *
     * @param cfg the static tenant configuration entry
     * @return the mapped DTO
     */
    @Mapping(target = "slug", source = "id")
    @Mapping(target = "hasApiKey", expression = "java(cfg.getApiKey() != null && !cfg.getApiKey().isBlank())")
    @Mapping(target = "active", expression = "java(cfg.getActive() != null ? cfg.getActive() : Boolean.TRUE)")
    @Mapping(target = "editable", expression = "java(false)")
    TenantDTO configToDto(TenantProperties.TenantConfig cfg);

    /**
     * Creates a new {@link Tenant} entity from a create/update DTO.
     * Sensitive fields are excluded and must be set separately.
     *
     * @param dto the create/update DTO
     * @return a new (unsaved) {@link Tenant} entity
     */
    @Mapping(target = "smtpPassword", ignore = true)
    @Mapping(target = "gitToken", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    Tenant fromDto(CreateUpdateTenantDTO dto);

    /**
     * Updates an existing {@link Tenant} entity in-place from a create/update DTO.
     * The slug is immutable and excluded; sensitive fields are handled separately.
     *
     * @param dto    the update DTO
     * @param tenant the existing entity to update
     */
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "smtpPassword", ignore = true)
    @Mapping(target = "gitToken", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    void updateFromDto(CreateUpdateTenantDTO dto, @MappingTarget Tenant tenant);

    /**
     * Creates a new {@link Tenant} entity from a static configuration entry.
     * Maps the config {@code id} to {@code slug}; {@code active} defaults to {@code true} when absent.
     * Sensitive fields ({@code smtpPassword}, {@code gitToken}, {@code apiKeyHash}) are excluded
     * and handled separately by {@link com.encircle360.oss.straightmail.service.TenantService}.
     *
     * @param cfg the static tenant configuration
     * @return a new (unsaved) {@link Tenant} entity
     */
    @Mapping(target = "slug", source = "id")
    @Mapping(target = "smtpPassword", ignore = true)
    @Mapping(target = "gitToken", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    @Mapping(target = "active", expression = "java(cfg.getActive() != null ? cfg.getActive() : Boolean.TRUE)")
    Tenant fromConfig(TenantProperties.TenantConfig cfg);

    /**
     * Updates an existing {@link Tenant} entity from a static configuration entry.
     * The slug is immutable; {@code active} and {@code apiKeyHash} are excluded and applied
     * conditionally by {@link com.encircle360.oss.straightmail.service.TenantService#upsertFromConfig}
     * so that absent values leave the existing database state unchanged.
     *
     * @param cfg    the static tenant configuration
     * @param tenant the existing entity to update
     */
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "smtpPassword", ignore = true)
    @Mapping(target = "gitToken", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateFromConfig(TenantProperties.TenantConfig cfg, @MappingTarget Tenant tenant);
}
