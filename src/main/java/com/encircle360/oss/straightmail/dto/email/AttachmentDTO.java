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

    @Schema(name = "filename", description = "Name of the file attached to an email")
    private String filename;

    @Schema(name = "content", description = "Base64 encoded content of the file")
    private String content;

    @Schema(name = "mimeType", description = "MimeType of the email")
    private String mimeType;
}
