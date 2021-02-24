package com.encircle360.oss.straightmail.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.encircle360.oss.straightmail.dto.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateFileRequestDTO;
import com.encircle360.oss.straightmail.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class MailController {

    private final EmailService emailService;

    @Deprecated
    @PostMapping("")
    @Operation(
        operationId = "sendEMailWithTemplateIdOldPath",
        description = "Endpoint to send emails via client, DEPRECATED! Please use: /email",
        deprecated = true
    )
    public ResponseEntity<EmailResultDTO> sendEMailWithTemplateIdOldPath(@RequestBody @Valid EmailTemplateFileRequestDTO emailFileRequest) {
        return send(emailFileRequest);
    }

    @PostMapping("/email")
    @Operation(operationId = "sendEMailWithTemplateId", description = "Endpoint to send emails via client")
    public ResponseEntity<EmailResultDTO> sendEMailWithTemplateId(@RequestBody @Valid EmailTemplateFileRequestDTO emailFileRequest) {
        return send(emailFileRequest);
    }

    @PostMapping("/email/inline")
    @Operation(operationId = "sendEMailWithInlineTemplate", description = "Sends an email with the given contents from request")
    public ResponseEntity<EmailResultDTO> sendEMailWithInlineTemplate(@RequestBody @Valid EmailInlineTemplateRequestDTO emailRequestInlineTemplateDTO) {
        return send(emailRequestInlineTemplateDTO);
    }

    @Deprecated
    @PostMapping("/inline")
    @Operation(
        operationId = "sendEMailWithInlineTemplateOldPath",
        description = "Sends an email with the given contents from request, DEPERCATED! Please use: /email/inline endpoint.",
        deprecated = true
    )
    public ResponseEntity<EmailResultDTO> sendEMailWithInlineTemplateOldPath(@RequestBody @Valid EmailInlineTemplateRequestDTO emailRequestInlineTemplateDTO) {
        return send(emailRequestInlineTemplateDTO);
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
