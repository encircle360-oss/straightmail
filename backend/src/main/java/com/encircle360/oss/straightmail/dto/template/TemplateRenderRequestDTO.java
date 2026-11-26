package com.encircle360.oss.straightmail.dto.template;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;

/**
 * Request DTO for rendering a template without sending an email.
 *
 * <p>Used with {@code POST /v1/render}. The template is resolved by {@code templateId}
 * and rendered with the provided model data. The rendered HTML and plain-text are returned
 * as a {@link RenderedTemplateDTO}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TemplateRenderRequest", description = "Request object for rendering a template from database")
public class TemplateRenderRequestDTO {

    @Schema(description = "Contains the id of the template")
    private String templateId;

    @Schema(description = "Contains contents for template, map key will be available in template")
    private HashMap<String, JsonNode> model;

}
