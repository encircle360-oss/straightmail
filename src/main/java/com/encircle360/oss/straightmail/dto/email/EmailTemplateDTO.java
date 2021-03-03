package com.encircle360.oss.straightmail.dto.email;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailTemplate", description = "Template which should be used to render the template into HTML")
public class EmailTemplateDTO {

    @NotBlank
    @Schema(name = "id", description = "Name of the template without file ending", example = "default")
    private String id;

    @Schema(name = "locale", description = "Locale as string", example = "de")
    private String locale;
}
