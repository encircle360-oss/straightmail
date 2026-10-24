package com.encircle360.oss.straightmail.dto.template;

import com.encircle360.oss.straightmail.dto.email.DetailedEmailResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO containing the rendered output of a FreeMarker template.
 *
 * <p>Returned by {@code POST /v1/render} and included in {@link DetailedEmailResultDTO}
 * when the send request has {@code verbose=true}. Contains the fully rendered HTML body,
 * optional plain-text, template metadata, and locale.
 */
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
