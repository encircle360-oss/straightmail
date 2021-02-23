package com.encircle360.oss.straightmail.dto.template;

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

    @NotBlank
    @Schema(name = "content", description = "Content of the template in database")
    private String content;
}
