package com.encircle360.oss.straightmail.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
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
