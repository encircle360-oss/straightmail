package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateDTO;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final Configuration freemarkerConfiguration;

    private final JavaMailSender emailClient;

    public EmailResultDTO sendMail(EmailRequestDTO emailRequestDTO) {
        String body = null;
        try {
            body = parseTemplate(emailRequestDTO.getEmailTemplate(), emailRequestDTO.getModel());
        } catch (IOException | TemplateException e) {
            return EmailResultDTO.builder()
                .message("Error parsing Template")
                .success(true)
                .build();
        }

        MimeMessage message = createMessage(emailRequestDTO, body);
        if (message == null) {
            return EmailResultDTO
                .builder()
                .message("Error creating mimetype message, maybe some missing or invalid fields")
                .success(false)
                .build();
        }

        emailClient.send(message);

        return EmailResultDTO.builder()
            .message("Message was send to SMTP Server")
            .success(true)
            .build();
    }

    private MimeMessage createMessage(EmailRequestDTO emailRequest, String body) {
        MimeMessage message = emailClient.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());
            helper.setFrom(emailRequest.getSender());
            helper.setTo(emailRequest.getRecipient());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(body, true);
        } catch (Exception ignored) { return null; }

        return message;
    }

    private String parseTemplate(EmailTemplateDTO emailTemplateDTO, HashMap<String, String> model) throws IOException, TemplateException {
        if (emailTemplateDTO == null) {
            return null;
        }

        ModelMap modelMap = new ModelMap();
        if (model != null) {
            modelMap.addAllAttributes(model);
        }

        String templatePath = emailTemplateDTO.getId() + ".ftl";

        Template template = freemarkerConfiguration.getTemplate(templatePath);
        if (emailTemplateDTO.getLocale() != null) {
            template.addAutoImport("spring", "spring.ftl");
            template.setLocale(Locale.forLanguageTag(emailTemplateDTO.getLocale()));
        }
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }
}