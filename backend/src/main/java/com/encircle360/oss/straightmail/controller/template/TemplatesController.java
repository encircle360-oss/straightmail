package com.encircle360.oss.straightmail.controller.template;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.mapper.TemplateMapper;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.TemplateService;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for write operations on database-backed email templates.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * All operations are automatically scoped to the current tenant via {@link TenantContext}.
 *
 * <p>Read operations (list, get) are provided by {@link TemplateReadController} which is always active.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/templates")
@Profile(DatabaseConfig.PROFILE)
@Tag(name = "Templates", description = "Template write operations (database only)")
public class TemplatesController {

    private final TemplateService templateService;
    private final TenantContext tenantContext;

    private static final TemplateMapper templateMapper = TemplateMapper.INSTANCE;

    /**
     * Creates a new template in the database for the current tenant.
     *
     * @param createUpdateTemplateDTO the template data to persist
     * @return {@code 201 CREATED} with the persisted {@link TemplateDTO}
     */
    @Operation(operationId = "createTemplate", description = "Creates a template in database with the given contents")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDTO> create(@RequestBody @Valid final CreateUpdateTemplateDTO createUpdateTemplateDTO) {
        Template template = templateMapper.createFromDto(createUpdateTemplateDTO);
        template = templateService.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(templateMapper.toDto(template));
    }

    /**
     * Updates an existing database template by its UUID.
     *
     * @param id                      the template UUID as a string
     * @param createUpdateTemplateDTO the new template data
     * @return {@code 200 OK} with the updated {@link TemplateDTO}, or {@code 404 NOT FOUND}
     */
    @Operation(operationId = "updateTemplate", description = "Updates a template in database with the given contents")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDTO> update(@PathVariable final String id, @RequestBody @Valid final CreateUpdateTemplateDTO createUpdateTemplateDTO) {
        Template template = templateService.get(id);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        templateMapper.updateFromDto(createUpdateTemplateDTO, template);
        template = templateService.save(template);
        return ResponseEntity.ok(templateMapper.toDto(template));
    }

    /**
     * Deletes a database template by its UUID.
     *
     * @param id the template UUID as a string
     * @return {@code 204 NO CONTENT} on success, or {@code 404 NOT FOUND}
     */
    @Operation(operationId = "deleteTemplate", description = "Deletes a template in database with the given id")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable final String id) {
        Template template = templateService.get(id);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        templateService.delete(template);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
