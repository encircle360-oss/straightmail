package com.encircle360.oss.straightmail.dto.email;

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
@Schema(name = "EmailTemplateFileRequest", description = "An email request with a template file id")
public class EmailTemplateFileRequestDTO extends EmailRequestDTO {

    @Schema(description = "Email template reference definition")
    private String emailTemplateId;
}
