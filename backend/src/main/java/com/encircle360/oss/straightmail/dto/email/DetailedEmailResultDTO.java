package com.encircle360.oss.straightmail.dto.email;

import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Extended email result DTO returned when the send request has {@code verbose=true}.
 *
 * <p>Extends {@link EmailResultDTO} with the rendered email subject and a
 * {@link RenderedTemplateDTO} containing the final HTML and plain-text content
 * that was dispatched to the SMTP server.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "DetailedEmailResult", description = "Detailed result of email processing.")
public class DetailedEmailResultDTO extends EmailResultDTO {

    @Schema(description = "Subject of the sent email.")
    private String subject;

    @Schema(description = "Contains the rendered HTML and plaintext email")
    private RenderedTemplateDTO renderResult;
}
