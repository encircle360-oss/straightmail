package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailResult", description = "Result of sending email")
public class EmailResultDTO {

    @Schema(name = "message", description = "")
    private String message;

    @Schema(name = "success", description = "")
    private boolean success;

}
