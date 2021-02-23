package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;

import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.support.RequestContext;

import com.encircle360.oss.straightmail.dto.AttachmentDTO;
import com.encircle360.oss.straightmail.dto.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateFileRequestDTO;
import com.encircle360.oss.straightmail.dto.FakeLocaleHttpServletRequest;
import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;
import com.fasterxml.jackson.databind.JsonNode;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.default-template}")
    private final String DEFAULT_TEMPLATE = null;

    @Value("${spring.mail.default-sender}")
    private final String DEFAULT_SENDER = null;

    private final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();

    private final Configuration freemarkerConfiguration;

    private final JsonNodeObjectWrapper jsonNodeObjectWrapper;

    private final JavaMailSender emailClient;

    private final ServletContext context;

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
                subject = parseTemplateFromFile(templateId + "_subject", emailRequest.getLocale(), emailRequest.getModel());
                body = parseTemplateFromFile(templateId, emailRequest.getLocale(), emailRequest.getModel());
                if (templateExists(templateId)) {
                    plainText = parseTemplateFromFile(templateId + "_subject", emailRequest.getLocale(), emailRequest.getModel());
                }
            } else if (emailRequest instanceof EmailInlineTemplateRequestDTO) {
                EmailInlineTemplateRequestDTO inlineTemplateRequest = (EmailInlineTemplateRequestDTO) emailRequest;
                subject = parseTemplateFromString(inlineTemplateRequest.getSubject(), inlineTemplateRequest.getLocale(), inlineTemplateRequest.getModel());
                body = parseTemplateFromString(inlineTemplateRequest.getEmailTemplate(), inlineTemplateRequest.getLocale(), inlineTemplateRequest.getModel());
                if (inlineTemplateRequest.getPlainText() != null) {
                    plainText = parseTemplateFromString(inlineTemplateRequest.getPlainText(), inlineTemplateRequest.getLocale(), inlineTemplateRequest.getModel());
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

    private MimeMessage createMessage(EmailRequestDTO emailRequest, String subject, String htmlBody) {
        return createMessage(emailRequest, subject, htmlBody, null);
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

    private String parseTemplateFromString(String emailTemplateString, String locale, HashMap<String, JsonNode> model) throws IOException, TemplateException {
        ModelMap modelMap = toModelMap(model);

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        freemarkerConfiguration.setObjectWrapper(jsonNodeObjectWrapper);
        Template template = new Template("email", new StringReader(emailTemplateString), freemarkerConfiguration);
        return processTemplate(template, locale, modelMap);
    }

    private String parseTemplateFromFile(String emailTemplateId, String locale, HashMap<String, JsonNode> model) throws IOException, TemplateException {
        ModelMap modelMap = toModelMap(model);

        if (emailTemplateId == null) {
            emailTemplateId = DEFAULT_TEMPLATE;
        }

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        String templatePath = emailTemplateId + ".ftl";

        freemarkerConfiguration.setObjectWrapper(jsonNodeObjectWrapper);
        Template template = freemarkerConfiguration.getTemplate(templatePath);

        return processTemplate(template, locale, modelMap);
    }

    private String processTemplate(Template template, String locale, ModelMap modelMap) throws IOException, TemplateException {

        template.setLocale(Locale.forLanguageTag(locale));

        // add import of spring macros, so we can use <@spring.messages 'x' /> in our templates
        template.addAutoImport("spring", "spring.ftl");

        // add macro request context, otherwise spring import will not work
        modelMap.addAttribute("springMacroRequestContext",
            new RequestContext(new FakeLocaleHttpServletRequest(locale), context));

        // process to string and return
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }

    private ModelMap toModelMap(HashMap<String, JsonNode> model) {
        ModelMap modelMap = new ModelMap();
        if (model == null) {
            return modelMap;
        }
        modelMap.addAllAttributes(model);
        return modelMap;
    }

    private EmailResultDTO result(String message, boolean success) {
        return EmailResultDTO.builder()
            .message(message)
            .success(success)
            .build();
    }

    public boolean templateExists(String templateId) {
        String templatePath = templateId + ".ftl";
        return new ClassPathResource("templates/" + templatePath).exists();
    }
}
