package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a file attachment to be included in an outgoing email.
 *
 * <p>The file content must be Base64-encoded. The MIME type is used to set the
 * {@code Content-Type} of the attachment part in the MIME message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Attachment", description = "A file attached to an email")
public class AttachmentDTO {

    @Schema(description = "Name of the file attached to an email")
    private String filename;

    @Schema(description = "Base64 encoded content of the file")
    private String content;

    @Schema(description = "MimeType of the email")
    private String mimeType;
}
