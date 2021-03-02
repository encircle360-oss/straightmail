package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.encircle360.oss.straightmail.dto.AttachmentDTO;
import com.encircle360.oss.straightmail.dto.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateFileRequestDTO;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.TemplateLoader;

import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.default-sender}")
    private final String DEFAULT_SENDER = null;

    private final JavaMailSender emailClient;

    private final FreemarkerService freemarkerService;

    private final TemplateLoader templateLoader;

    private final Base64.Decoder decoder = Base64.getDecoder();

    public <T extends EmailRequestDTO> EmailResultDTO sendMail(T emailRequest) {
        if (emailRequest == null) {
            return result("Request was empty", false);
        }

        String plainText = null;
        String body = null;
        String subject = null;

        try {
            if (emailRequest instanceof EmailTemplateFileRequestDTO) {
                String templateId = ((EmailTemplateFileRequestDTO) emailRequest).getEmailTemplateId();
                Template template = templateLoader.loadTemplate(templateId);

                subject = freemarkerService.parseTemplateFromString(template.getSubject(), emailRequest.getLocale(), emailRequest.getModel());
                body = freemarkerService.parseTemplateFromString(template.getHtml(), emailRequest.getLocale(), emailRequest.getModel());
                if (freemarkerService.templateExists(templateId)) {
                    plainText = freemarkerService.parseTemplateFromString(template.getPlain(), emailRequest.getLocale(), emailRequest.getModel());
                }
            } else if (emailRequest instanceof EmailInlineTemplateRequestDTO) {
                EmailInlineTemplateRequestDTO inlineTemplateRequest = (EmailInlineTemplateRequestDTO) emailRequest;
                subject = freemarkerService.parseTemplateFromString(inlineTemplateRequest.getSubject(), inlineTemplateRequest.getLocale(), inlineTemplateRequest.getModel());
                body = freemarkerService.parseTemplateFromString(inlineTemplateRequest.getEmailTemplate(), inlineTemplateRequest.getLocale(), inlineTemplateRequest.getModel());
                if (inlineTemplateRequest.getPlainText() != null) {
                    plainText = freemarkerService.parseTemplateFromString(inlineTemplateRequest.getPlainText(), inlineTemplateRequest.getLocale(), inlineTemplateRequest
                        .getModel());
                }
            }
        } catch (IOException | TemplateException e) {
            log.error(e.getMessage());
        }

        if (subject == null || body == null) {
            return result("Error parsing Template", false);
        }

        // remove all HTML from subject
        subject = Jsoup.clean(subject, Whitelist.none());

        MimeMessage message = createMessage(emailRequest, subject, body, plainText);
        if (message == null) {
            return result("Error creating mimetype message, maybe some missing or invalid fields", false);
        }

        emailClient.send(message);

        return result("Message was send to SMTP Server", true);
    }

    private MimeMessage createMessage(EmailRequestDTO emailRequest, String subject, String htmlBody, String plainText) {
        MimeMessage message = emailClient.createMimeMessage();

        if (emailRequest.getSender() == null) {
            emailRequest.setSender(DEFAULT_SENDER);
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

            // set plain text result by removing all html tags and convert br to \n
            if (plainText == null) {
                plainText = Jsoup.clean(htmlBody, Whitelist.none().addTags("br"));
                plainText = plainText.replaceAll("(<br>|<br/>|<br\\s+/>)", "\n");
            }

            helper.setFrom(emailRequest.getSender());
            helper.setSubject(subject);

            if (emailRequest.getRecipients() != null && !emailRequest.getRecipients().isEmpty()) {
                for (String s : emailRequest.getRecipients()) {
                    helper.addTo(s);
                }
            }

            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                for (String s : emailRequest.getCc()) {
                    helper.addCc(s);
                }
            }

            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                for (String s : emailRequest.getBcc()) {
                    helper.addBcc(s);
                }
            }

            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
                for (AttachmentDTO attachment : emailRequest.getAttachments()) {
                    byte[] fileBytes = decoder.decode(attachment.getContent());
                    ByteArrayDataSource attachmentByteArrayDataSource = new ByteArrayDataSource(fileBytes, attachment.getMimeType());
                    helper.addAttachment(attachment.getFilename(), attachmentByteArrayDataSource);
                }
            }

            helper.setText(plainText, htmlBody);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }

        return message;
    }

    private EmailResultDTO result(String message, boolean success) {
        return EmailResultDTO.builder()
            .message(message)
            .success(success)
            .build();
    }
}
