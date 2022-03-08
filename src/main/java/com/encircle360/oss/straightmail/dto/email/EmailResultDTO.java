package com.encircle360.oss.straightmail.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailResult", description = "Result of email processing.")
public class EmailResultDTO {

    @Schema(description = "Includes information about the status of the email.")
    private String message;

    @Schema(description = "Shows if message was sent or not")
    private boolean success;

}
