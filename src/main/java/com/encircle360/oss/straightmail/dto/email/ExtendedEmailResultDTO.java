package com.encircle360.oss.straightmail.dto.email;

import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "EmailResult", description = "Result of sending email")
public class ExtendedEmailResultDTO extends EmailResultDTO {

    @Schema(name = "message", description = "")
    private String message;

    @Schema(name = "success", description = "")
    private boolean success;

    @Schema(description = "Contains the rendered HTML and plaintext email")
    private RenderedTemplateDTO renderResult;
}
