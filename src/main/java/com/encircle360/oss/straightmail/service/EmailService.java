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
import org.springframework.context.MessageSource;
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

    private final JavaMailSender emailClient;

    private final ServletContext context;

    private final MessageSource messageSource;

    public <T extends EmailRequestDTO> EmailResultDTO sendMail(T emailRequestDTO) {
        if (emailRequestDTO == null) {
            return EmailResultDTO.builder()
                .message("Request was empty")
                .success(false)
                .build();
        }

        String body = null;
        try {
            if (emailRequestDTO instanceof EmailTemplateFileRequestDTO) {
                body = parseTemplateFromFile(((EmailTemplateFileRequestDTO) emailRequestDTO).getEmailTemplateId(), emailRequestDTO.getLocale(), emailRequestDTO.getModel());
            } else if (emailRequestDTO instanceof EmailInlineTemplateRequestDTO) {
                body = parseTemplateFromString(((EmailInlineTemplateRequestDTO) emailRequestDTO).getEmailTemplate(), emailRequestDTO.getLocale(), emailRequestDTO.getModel());
            }
        } catch (IOException | TemplateException e) {
            log.error(e.getMessage());
        }

        if (body == null) {
            return EmailResultDTO.builder()
                .message("Error parsing Template")
                .success(false)
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

    private String messageOrDefault(String subject, String locale) {
        try {
            return messageSource.getMessage(subject, null, Locale.forLanguageTag(locale));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return subject;
    }

    private MimeMessage createMessage(EmailRequestDTO emailRequest, String body) {
        MimeMessage message = emailClient.createMimeMessage();

        if (emailRequest.getSender() == null) {
            emailRequest.setSender(DEFAULT_SENDER);
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

            // set plain text result by removing all html tags and convert br to \n
            String plainText = Jsoup.clean(body, Whitelist.none().addTags("br"));
            plainText = plainText.replaceAll("(<br>|<br/>|<br\\s+/>)", "\n");
            String subject = messageOrDefault(emailRequest.getSubject(), emailRequest.getLocale());

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
                Base64.Decoder decoder = Base64.getDecoder();
                for (AttachmentDTO attachment : emailRequest.getAttachments()) {
                    byte[] fileBytes = decoder.decode(attachment.getContent());
                    ByteArrayDataSource attachmentByteArrayDataSource = new ByteArrayDataSource(fileBytes, attachment.getMimeType());
                    helper.addAttachment(attachment.getFilename(), attachmentByteArrayDataSource);
                }
            }

            helper.setText(plainText, body);
        } catch (Exception ignored) {
            return null;
        }

        return message;
    }

    private String parseTemplateFromString(String emailTemplateString, String locale, HashMap<String, JsonNode> model) throws IOException, TemplateException {
        ModelMap modelMap = toModelMap(model);

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

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
        if (model != null) {
            for (String s : model.keySet()) {
                JsonNode node = model.get(s);
                switch (node.getNodeType()) {
                    case NULL:
                        break;
                    case STRING:
                        modelMap.addAttribute(s, node.textValue());
                        break;
                    case BOOLEAN:
                        modelMap.addAttribute(s, node.booleanValue());
                        break;
                    case NUMBER:
                        modelMap.addAttribute(s, node.numberValue());
                        break;
                    default:
                        modelMap.addAttribute(s, node);
                        break;
                }
            }
        }
        return modelMap;
    }
}
