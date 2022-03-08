package com.encircle360.oss.straightmail.dto.template;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
