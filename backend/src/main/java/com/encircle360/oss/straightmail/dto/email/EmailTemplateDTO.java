package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that identifies a FreeMarker email template by its ID and an optional locale.
 *
 * <p>Used to specify which template should be rendered, with the locale determining
 * which language variant of Spring message bundles is applied during rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailTemplate", description = "Template which should be used to render the template into HTML")
public class EmailTemplateDTO {

    @NotBlank
    @Schema(description = "Name of the template without file ending", example = "default")
    private String id;

    @Schema(description = "Locale as string", example = "de")
    private String locale;
}
