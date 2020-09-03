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

    @PostMapping("")
    @Operation(operationId = "requestMail", description = "Endpoint to send emails via client")
    public ResponseEntity<EmailResultDTO> requestMail(@RequestBody @Valid EmailTemplateFileRequestDTO emailRequest) {
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

    @PostMapping("/inline")
    public ResponseEntity<EmailResultDTO> requestInlineTemplateMail(@RequestBody @Valid EmailInlineTemplateRequestDTO emailRequestInlineTemplateDTO) {
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
