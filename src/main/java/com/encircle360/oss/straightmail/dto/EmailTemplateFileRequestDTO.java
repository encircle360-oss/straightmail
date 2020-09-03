package com.encircle360.oss.straightmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailTemplateFileRequestDTO extends EmailRequestDTO {

    @Schema(name = "emailTemplate", description = "Email template reference definition")
    private EmailTemplateDTO emailTemplate;
}
