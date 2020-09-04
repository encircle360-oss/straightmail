package com.encircle360.oss.straightmail.dto;

import javax.validation.constraints.NotBlank;

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
@Schema(name = "EmailInlineTemplateRequest", description = "An email request which includes the template as string")
public class EmailInlineTemplateRequestDTO extends EmailRequestDTO {

    @NotBlank
    @Schema(name = "subject", description = "The subject of the email which will be send", example = "This is an urgent E-Mail")
    private String subject;

    @Schema(name = "emailTemplate", description = "Email template reference definition")
    private String emailTemplate;

}
