package com.encircle360.oss.straightmail.controller.mail;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import com.encircle360.oss.straightmail.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes endpoints for sending emails.
 *
 * <p>Provides two sending strategies:
 * <ul>
 *   <li>{@code POST /v1/email} — resolves the email template by ID from the configured template source.</li>
 *   <li>{@code POST /v1/email/inline} — accepts an inline FreeMarker template in the request body.</li>
 * </ul>
 *
 * <p>Both endpoints delegate to {@link EmailService#sendMail} and return an
 * {@link EmailResultDTO} indicating success or failure.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class MailController {

    private final EmailService emailService;

    /**
     * Sends an email by resolving the template from the configured template source (file or database) by its ID.
     *
     * @param emailFileRequest the send request containing the template ID, recipients, and model data
     * @return {@code 200 OK} with the send result, or {@code 400 BAD REQUEST} / {@code 500} on failure
     */
    @PostMapping("/v1/email")
    @Operation(operationId = "sendEMailWithTemplateId", description = "Endpoint to send emails via client")
    public ResponseEntity<EmailResultDTO> sendEMailWithTemplateId(@RequestBody @Valid EmailTemplateFileRequestDTO emailFileRequest) {
        return this.send(emailFileRequest);
    }

    /**
     * Sends an email using an inline FreeMarker template provided directly in the request body.
     *
     * @param emailRequestInlineTemplateDTO the send request containing the inline subject, HTML body,
     *                                      optional plain-text, recipients, and model data
     * @return {@code 200 OK} with the send result, or {@code 400 BAD REQUEST} / {@code 500} on failure
     */
    @PostMapping("/v1/email/inline")
    @Operation(operationId = "sendEMailWithInlineTemplate", description = "Sends an email with the given contents from request")
    public ResponseEntity<EmailResultDTO> sendEMailWithInlineTemplate(@RequestBody @Valid EmailInlineTemplateRequestDTO emailRequestInlineTemplateDTO) {
        return this.send(emailRequestInlineTemplateDTO);
    }

    private ResponseEntity<EmailResultDTO> send(EmailRequestDTO emailRequest) {
        EmailResultDTO emailResult;
        try {
            emailResult = emailService.sendMail(emailRequest);
        } catch (MailException mailException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EmailResultDTO
                    .builder()
                    .success(false)
                    .message("Sending email gone wrong: " + mailException.getMessage())
                    .build());
        }

        if (!emailResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emailResult);
        }
        return ResponseEntity.status(HttpStatus.OK).body(emailResult);
    }
}
