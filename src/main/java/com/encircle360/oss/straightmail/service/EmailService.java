package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.support.RequestContext;

import com.encircle360.oss.straightmail.dto.EmailRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateDTO;
import com.encircle360.oss.straightmail.dto.FakeLocaleHttpServletRequest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final String DEFAULT_TEMPLATE = "default";

    @Value("${spring.mail.default-sender}")
    private final String DEFAULT_SENDER = null;

    private final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();

    private final Configuration freemarkerConfiguration;

    private final JavaMailSender emailClient;

    private final ServletContext context;

    public EmailResultDTO sendMail(EmailRequestDTO emailRequestDTO) {
        String body;
        try {
            body = parseTemplate(emailRequestDTO.getEmailTemplate(), emailRequestDTO.getModel());
        } catch (IOException | TemplateException e) {
            return EmailResultDTO.builder()
                .message("Error parsing Template: " + e.getMessage())
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

    private MimeMessage createMessage(EmailRequestDTO emailRequest, String body) {
        MimeMessage message = emailClient.createMimeMessage();
        MimeMessageHelper helper;
        if (emailRequest.getSender() == null) {
            emailRequest.setSender(DEFAULT_SENDER);
        }

        try {
            helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());
            helper.setFrom(emailRequest.getSender());
            helper.setTo(emailRequest.getRecipient());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(body, true);
        } catch (Exception ignored) {
            return null;
        }

        return message;
    }

    private String parseTemplate(EmailTemplateDTO emailTemplateDTO, HashMap<String, String> model) throws IOException, TemplateException {
        ModelMap modelMap = new ModelMap();
        if (model != null) {
            modelMap.addAllAttributes(model);
        }

        if (emailTemplateDTO == null) {
            emailTemplateDTO = EmailTemplateDTO
                .builder()
                .id(DEFAULT_TEMPLATE)
                .locale(DEFAULT_LOCALE)
                .build();
        }

        String templatePath = emailTemplateDTO.getId() + ".ftl";

        Template template = freemarkerConfiguration.getTemplate(templatePath);
        template.setLocale(Locale.forLanguageTag(emailTemplateDTO.getLocale()));

        // add import of spring macros, so we can use <@spring.messages 'x' /> in our templates
        template.addAutoImport("spring", "spring.ftl");

        // add macro request context, otherwise spring import will not work
        modelMap.addAttribute("springMacroRequestContext", new RequestContext(new FakeLocaleHttpServletRequest(
            emailTemplateDTO == null ? null : emailTemplateDTO.getLocale()), context));

        // process to string and return
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }
}