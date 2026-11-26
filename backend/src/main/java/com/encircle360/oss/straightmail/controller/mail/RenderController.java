package com.encircle360.oss.straightmail.controller.mail;

import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateRenderRequestDTO;
import com.encircle360.oss.straightmail.mapper.TemplateMapper;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.FreemarkerService;
import com.encircle360.oss.straightmail.service.template.loader.TemplateLoader;
import com.encircle360.oss.straightmail.service.template.provider.CompositeTemplateProvider;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller for rendering FreeMarker email templates without sending them.
 *
 * <p>Exposes {@code POST /v1/render} which loads a template by ID and returns the rendered
 * HTML and plain-text content as a {@link RenderedTemplateDTO}. Useful for previewing
 * template output before sending.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/render")
public class RenderController {

    private static final TemplateMapper mapper = TemplateMapper.INSTANCE;

    private final ObjectProvider<TemplateLoader> templateLoader;
    private final ObjectProvider<CompositeTemplateProvider> compositeTemplateProvider;
    private final ObjectProvider<TenantContext> tenantContext;

    private final FreemarkerService freemarkerService;

    /**
     * Renders the template identified by the request's template ID with the provided model data.
     *
     * @param templateRenderRequestDTO the render request containing the template ID and model
     * @return {@code 200 OK} with the rendered HTML and plain-text content;
     * {@code 404 NOT FOUND} if the template does not exist;
     * {@code 422 UNPROCESSABLE CONTENT} with {@code {"message": "..."}} if FreeMarker reports a
     * template error (invalid syntax, missing variable, etc.) — the message is safe to display
     * to the developer
     */
    @Operation(operationId = "renderTemplate", description = "Returns the rendered template as HTML.")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> render(@RequestBody @Valid TemplateRenderRequestDTO templateRenderRequestDTO) {
        Template template = this.resolveTemplate(templateRenderRequestDTO.getTemplateId());
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            String renderedHtml = freemarkerService.renderTemplateToString(template.getHtml(), template.getLocale(), templateRenderRequestDTO.getModel());
            String renderedPlain = freemarkerService.renderTemplateToString(template.getPlain(), template.getLocale(), templateRenderRequestDTO.getModel());

            RenderedTemplateDTO rendered = mapper.toRendered(template, renderedHtml, renderedPlain);
            return ResponseEntity.status(HttpStatus.OK).body(rendered);
        } catch (TemplateException | IOException e) {
            // Templates are rendered from in-memory strings, so both TemplateException (runtime
            // evaluation errors) and IOException (ParseException — invalid FreeMarker syntax) are
            // developer-relevant and safe to surface to the caller.
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", e.getMessage()));
        }
    }

    private Template resolveTemplate(String templateId) {
        if (templateId == null) return null;
        // Decode :: → / so that encoded file-path IDs (e.g. "templates::default") resolve correctly
        String name = templateId.replace("::", "/");

        CompositeTemplateProvider provider = compositeTemplateProvider.getIfAvailable();
        TenantContext ctx = tenantContext.getIfAvailable();
        if (provider != null && ctx != null) {
            return provider.resolve(ctx.getTenantId(), name).orElse(null);
        }

        TemplateLoader loader = templateLoader.getIfAvailable();
        return loader != null ? loader.loadTemplate(name) : null;
    }
}
