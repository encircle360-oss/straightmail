package com.encircle360.oss.straightmail.dto.email;

import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.encircle360.oss.straightmail.config.EmailRegex;
import com.fasterxml.jackson.databind.JsonNode;

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
@Schema(name = "EmailRequest", description = "Object for requesting an email sending")
public abstract class EmailRequestDTO {

    @NotNull
    @Schema(description = "The recipient of the send mail")
    private List<@Pattern(regexp = EmailRegex.value) String> recipients;

    @Schema(description = "The carbon copy recipients of the send mail")
    private List<@Pattern(regexp = EmailRegex.value) String> cc;

    @Schema(description = "The black carbon copy recipients of the send mail")
    private List<@Pattern(regexp = EmailRegex.value) String> bcc;

    @Schema(description = "Attachments on an email")
    private List<AttachmentDTO> attachments;

    @NotBlank
    @Pattern(regexp = EmailRegex.value)
    @Schema(description = "Sender of the email", example = "sender@encircle360.com")
    private String sender;

    @Schema(description = "Contains contents for template, map key will be available in template")
    private HashMap<String, JsonNode> model;

    @Schema(description = "Locale country code", example = "de")
    private String locale;

    @Builder.Default
    @Schema(description = "If set to true, result will contain render result, otherwise render result will be null")
    private boolean verbose = false;

}
