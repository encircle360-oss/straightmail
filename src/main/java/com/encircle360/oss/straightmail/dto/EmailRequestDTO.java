package com.encircle360.oss.straightmail.dto;

import java.util.HashMap;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailRequest", description = "Object for requesting an email sending")
public class EmailRequestDTO {

    @Email
    @NotBlank
    @Schema(name = "recipient", description = "The recipient of the send mail", example = "test@encircle360.com")
    private String recipient;

    @NotBlank
    @Schema(name = "subject", description = "The subject of the email which will be send", example = "This is an urgent E-Mail")
    private String subject;

    @Email
    @NotBlank
    @Schema(name = "sender", description = "Sender of the email", example = "sender@encircle360.com")
    private String sender;

    @Schema(name = "model", description = "Contains contents for template, map key will be available in template", example = "")
    private HashMap<String, String> model;

    @Schema(name = "emailTemplate", description = "Email template reference definition")
    private EmailTemplateDTO emailTemplate;
}
