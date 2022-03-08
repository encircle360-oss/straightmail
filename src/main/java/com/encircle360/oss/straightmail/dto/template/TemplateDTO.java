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
}
