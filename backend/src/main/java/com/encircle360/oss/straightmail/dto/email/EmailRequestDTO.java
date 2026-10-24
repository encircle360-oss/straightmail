package com.encircle360.oss.straightmail.dto.email;

import com.encircle360.oss.straightmail.config.EmailRegex;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;

/**
 * Abstract base class for all email send requests.
 *
 * <p>Contains common fields shared by both template-file-based and inline-template requests:
 * recipients (to, cc, bcc), sender, attachments, FreeMarker model data, locale, and a verbose flag.
 * Concrete subclasses add the template reference ({@link EmailTemplateFileRequestDTO}) or
 * inline template content ({@link EmailInlineTemplateRequestDTO}).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailRequest", description = "Object for requesting an email sending")
public abstract class EmailRequestDTO {

    @NotNull
    @Schema(description = "The recipient of the send mail", example = "recipient-1@encircle360.com, recipient-2@encircle360.com")
    private List<@Pattern(regexp = EmailRegex.value) String> recipients;

    @Schema(description = "The carbon copy recipients of the send mail", example = "cc-1@encircle360.com, cc-2@encircle360.com")
    private List<@Pattern(regexp = EmailRegex.value) String> cc;

    @Schema(description = "The black carbon copy recipients of the send mail", example = "bcc-1@encircle360.com, bcc-2@encircle360.com")
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
