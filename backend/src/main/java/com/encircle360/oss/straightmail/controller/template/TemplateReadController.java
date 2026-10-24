package com.encircle360.oss.straightmail.controller.template;

import com.encircle360.oss.straightmail.dto.PageContainer;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.mapper.TemplateMapper;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.TemplateService;
import com.encircle360.oss.straightmail.service.template.provider.CompositeTemplateProvider;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for reading email templates from all configured sources (database, Git, file).
 *
 * <p>Works in both database and non-database mode by delegating all reads to
 * {@link CompositeTemplateProvider}, which aggregates results from every active
 * {@link com.encircle360.oss.straightmail.service.template.provider.TemplateSourceProvider}.
 *
 * <p>In database mode, {@link TemplateService} is additionally used to look up templates by
 * their UUID primary key. In non-database mode the UUID lookup is skipped and all templates
 * are resolved by name through the composite provider.
 *
 * <p>All operations are automatically scoped to the current tenant via {@link TenantContext}.
 * Create, update, and delete operations are handled by {@link TemplatesController} (database-only).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/templates")
@Tag(name = "Templates", description = "Template read operations")
public class TemplateReadController {

    private final CompositeTemplateProvider compositeTemplateProvider;
    private final ObjectProvider<TemplateService> templateServiceProvider;
    private final TenantContext tenantContext;

    private static final TemplateMapper templateMapper = TemplateMapper.INSTANCE;

    /**
     * Returns a paginated list of templates from all enabled sources for the current tenant.
     *
     * <p>Pagination and sorting are applied in-memory after aggregating results from all providers.
     * Optionally filters by one or more tags.
     *
     * @param sort optional sort field name
     * @param size page size (default: 10)
     * @param page zero-based page index (default: 0)
     * @param tag  optional list of tags to filter by
     * @return {@code 200 OK} with a {@link PageContainer} of {@link TemplateDTO} objects
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listTemplates", description = "Returns a pageable list of templates from all sources")
    public ResponseEntity<PageContainer<TemplateDTO>> list(@RequestParam(required = false) String sort,
                                                           @RequestParam(required = false, defaultValue = "10") Integer size,
                                                           @RequestParam(required = false, defaultValue = "0") Integer page,
                                                           @RequestParam(required = false) List<String> tag) {
        Pageable pageable = PageRequest.of(page, size, sort == null ? Sort.unsorted() : Sort.by(sort));
        Page<TemplateView> templatePage = compositeTemplateProvider.listAll(tenantContext.getTenantId(), tag, pageable);

        List<TemplateDTO> dtos = templatePage.getContent().stream().map(templateMapper::toDto).toList();
        return ResponseEntity.ok(PageContainer.of(dtos, templatePage));
    }

    /**
     * Returns a template by its identifier, supporting both database UUIDs and encoded file/Git
     * template names. Non-database templates use {@code ::} as a path-separator substitute
     * (e.g. {@code "templates::default"} resolves to the file at {@code templates/default.ftl}).
     *
     * <p>Resolution order:
     * <ol>
     *   <li>UUID lookup via {@link TemplateService} (database mode only)</li>
     *   <li>Name-based lookup via {@link CompositeTemplateProvider} (all modes)</li>
     * </ol>
     *
     * @param id the template UUID or encoded name (slashes replaced with {@code ::})
     * @return {@code 200 OK} with the {@link TemplateDTO}, or {@code 404 NOT FOUND} if not found
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getTemplate", description = "Returns a template by its id or encoded name, if not found 404")
    public ResponseEntity<TemplateDTO> get(@PathVariable final String id) {
        TemplateService service = templateServiceProvider.getIfAvailable();
        if (service != null) {
            Template template = service.get(id);
            if (template != null) {
                return ResponseEntity.ok(templateMapper.toDto(template));
            }
        }

        // Decode :: → / and resolve as a file/Git template name, preserving source metadata
        TemplateView view = compositeTemplateProvider
                .resolveView(tenantContext.getTenantId(), id.replace("::", "/"))
                .orElse(null);
        if (view == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(templateMapper.toDto(view));
    }
}
