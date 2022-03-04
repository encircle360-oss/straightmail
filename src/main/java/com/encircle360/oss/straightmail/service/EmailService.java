package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import com.encircle360.oss.straightmail.dto.email.*;
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.TemplateLoader;
import com.encircle360.oss.straightmail.util.HtmlUtil;
import com.fasterxml.jackson.databind.JsonNode;

import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.default-sender}")
    private final String DEFAULT_SENDER = null;

    private final FreemarkerService freemarkerService;

    private final Base64.Decoder decoder = Base64.getDecoder();
    private final TemplateLoader templateLoader;
    private final JavaMailSender emailClient;

    public <T extends EmailRequestDTO> EmailResultDTO sendMail(T emailRequest) {
        if (emailRequest == null) {
            return result("Request was empty", null, false);
        }

        String plainText = null;
        String body = null;
        String subject = null;

        String locale = emailRequest.getLocale();
        HashMap<String, JsonNode> model = emailRequest.getModel();
        String templateId = null;
        String templateName = null;
        if (emailRequest instanceof EmailTemplateFileRequestDTO emailTemplateFileRequest) {
            templateId = emailTemplateFileRequest.getEmailTemplateId();
            Template template = templateLoader.loadTemplate(templateId);
            if (template == null) {
                return result("Template not found", null, false);
            }

            templateName = template.getName();
            subject = template.getSubject();
            body = template.getHtml();
            plainText = template.getPlain();
        } else if (emailRequest instanceof EmailInlineTemplateRequestDTO inlineTemplateRequest) {
            subject = inlineTemplateRequest.getSubject();
            body = inlineTemplateRequest.getEmailTemplate();
            plainText = inlineTemplateRequest.getPlainText();
        }

        try {
            subject = freemarkerService.parseTemplateFromString(subject, locale, model);
            body = freemarkerService.parseTemplateFromString(body, locale, model);
            plainText = freemarkerService.parseTemplateFromString(plainText, locale, model);
        } catch (IOException | TemplateException e) {
            log.error(e.getMessage());
        }

        if (subject == null || body == null) {
            return result("Error parsing Template", null, false);
        }

        MimeMessage message = createMessage(emailRequest, subject, body, plainText);
        if (message == null) {
            return result("Error creating mimetype message, maybe some missing or invalid fields",
                    null,
                    false);
        }

        emailClient.send(message);
        RenderedTemplateDTO renderedTemplateDTO = null;

        if (emailRequest.isVerbose()) {
            renderedTemplateDTO = RenderedTemplateDTO
                    .builder()
                    .html(body)
                    .plain(plainText)
                    .name(templateName)
                    .id(templateId)
                    .build();
        }

        return result("Message was send to SMTP Server", renderedTemplateDTO, true);
    }

    private MimeMessage createMessage(EmailRequestDTO emailRequest,
                                      String subject,
                                      String htmlBody,
                                      String plainText) {
        MimeMessage message = emailClient.createMimeMessage();

        // Do not send messages with null body or subject, must be set!
        if (subject == null || htmlBody == null) {
            return null;
        }

        // remove all HTML from subject
        subject = Jsoup.clean(subject, Whitelist.none());

        if (emailRequest.getSender() == null) {
            emailRequest.setSender(DEFAULT_SENDER);
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            // set plain text result by removing all html tags and convert br to \n
            if (plainText == null) {
                plainText = Jsoup.clean(htmlBody,
                        Whitelist.none().addTags("br", "a").addAttributes("a", "href"));
                plainText = plainText.replaceAll("(<br>|<br/>|<br\\s+/>)", "\n");
                plainText = HtmlUtil.replaceHtmlLinkToPlainText(plainText);
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
                    ByteArrayDataSource attachmentByteArrayDataSource = new ByteArrayDataSource(
                            fileBytes, attachment.getMimeType());
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

    private EmailResultDTO result(String message,
                                  RenderedTemplateDTO renderedTemplate,
                                  boolean success) {
        if (renderedTemplate != null) {
            return ExtendedEmailResultDTO
                    .builder()
                    .message(message)
                    .success(success)
                    .renderResult(renderedTemplate)
                    .build();
        }

        return EmailResultDTO.builder()
                .message(message)
                .success(success)
                .build();
    }
}
