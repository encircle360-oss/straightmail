package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Email send request that references an existing template by its ID.
 *
 * <p>The template is resolved from the configured template source (file, database, or Git)
 * by {@link com.encircle360.oss.straightmail.service.EmailService} at send time.
 * Used with {@code POST /v1/email}.
 */
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
