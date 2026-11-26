package com.encircle360.oss.straightmail.dto.template;

import com.encircle360.oss.straightmail.service.template.provider.TemplateSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Response DTO representing an email template from any enabled source.
 *
 * <p>Returned by {@code GET /v1/templates} and {@code GET /v1/templates/{id}}.
 * The {@code source} field identifies where the template originates (database, Git, or file).
 * The {@code editable} flag indicates whether the template can be modified via the API
 * (only {@link com.encircle360.oss.straightmail.service.template.provider.TemplateSource#DATABASE} templates are editable).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Template")
public class TemplateDTO {

    @Schema(description = "Id of the template in database")
    private String id;

    @Schema(description = "Subject of the template in database")
    private String subject;

    @Schema(description = "Name of the template in database")
    private String name;

    @Schema(description = "Html content of the template in database")
    private String html;

    @Schema(description = "Plain content of the template in database")
    private String plain;

    @Schema(description = "Locale of the template in database")
    private String locale;

    @Schema(description = "List of tags which this template has.")
    private List<String> tags;

    @Schema(description = "Source of the template (DATABASE, GIT, FILE)")
    private TemplateSource source;

    @Schema(description = "Whether this template can be edited via the API")
    private boolean editable;

    @Schema(description = "Git branch this template was synced from; null for DATABASE and FILE templates")
    private String gitBranch;
}
