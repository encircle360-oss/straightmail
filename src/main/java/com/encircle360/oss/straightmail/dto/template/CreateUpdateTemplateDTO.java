package com.encircle360.oss.straightmail.dto.template;

import java.util.List;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateUpdateTemplate", description = "Data for creating a template")
public class CreateUpdateTemplateDTO {

    @NotBlank
    @Schema(name = "name", description = "Name of the template in database")
    private String name;

    @Schema(name = "subject", description = "Subject of the template in database")
    private String subject;

    @NotBlank
    @Schema(name = "html", description = "Html content of the template in database")
    private String html;

    @Schema(name = "plain", description = "Plain content of the template in database")
    private String plain;

    @Schema(name = "locale", description = "Locale of the template in database")
    private String locale;

    @Schema(name = "tags", description = "List of tags which this template has.")
    private List<String> tags;

}
