package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
