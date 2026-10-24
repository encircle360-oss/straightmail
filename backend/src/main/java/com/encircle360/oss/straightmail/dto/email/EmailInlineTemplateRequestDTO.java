package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Email send request that carries the FreeMarker template inline in the request body.
 *
 * <p>The subject, HTML body, and optional plain-text are provided directly as FreeMarker template
 * strings. No template lookup is performed. Used with {@code POST /v1/email/inline}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "EmailInlineTemplateRequest", description = "An email request which includes the template as string")
public class EmailInlineTemplateRequestDTO extends EmailRequestDTO {

    @NotBlank
    @Schema(description = "The subject of the email which will be send", example = "This is an urgent E-Mail")
    private String subject;

    @Schema(description = "Email template reference definition")
    private String emailTemplate;

    @Schema(description = "Email template as plaintext without any html")
    private String plainText;

}
