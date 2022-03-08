package com.encircle360.oss.straightmail.dto.template;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RenderedTemplate", description = "template result with rendered content")
public class RenderedTemplateDTO {

    @Schema(description = "Id of the template")
    private String id;

    @Schema(description = "Name of the template")
    private String name;

    @Schema(description = "Html content of the result")
    private String html;

    @Schema(description = "Plain content of the result")
    private String plain;

    @Schema(description = "Locale of the result")
    private String locale;

    @Schema(description = "List of tags which this template has.")
    private List<String> tags;
}
