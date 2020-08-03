package com.encircle360.oss.straightmail.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MailController {

    private final EmailService emailService;

    @PostMapping("")
    public ResponseEntity<EmailResultDTO> requestMail(@RequestBody EmailRequestDTO emailRequestDTO) {
        EmailResultDTO emailResult;
        try {
            emailResult = emailService.sendMail(emailRequestDTO);
        } catch (MailException mailException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EmailResultDTO
                .builder()
                .success(false)
                .message("Sending email gone wrong")
                .build());
        }

        if(!emailResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emailResult);
        }
        return ResponseEntity.status(HttpStatus.OK).body(emailResult);
    }
}
