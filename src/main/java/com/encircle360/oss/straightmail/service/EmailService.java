package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.util.HashMap;

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

    public EmailResultDTO sendMail(EmailRequestDTO emailRequestDTO) {
        try {
            String body = parseTemplate(emailRequestDTO.getEmailTemplate(), emailRequestDTO.getModel());
        } catch (IOException | TemplateException e) {
            return EmailResultDTO.builder()
                .status(500)
                .message("Error parsing Template")
                .build();
        }

        //TODO implement sending of mails

        return EmailResultDTO.builder()
            .status(250)
            .message("success")
            .build();
    }

    private String parseTemplate(EmailTemplateDTO emailTemplateDTO, HashMap<String, String> model) throws IOException, TemplateException {
        if (emailTemplateDTO == null) {
            return null;
        }

        ModelMap modelMap = new ModelMap();
        if (model != null) {
            modelMap.addAllAttributes(model);
        }

        String templatePath;
        if (emailTemplateDTO.getLocale() != null) {
            templatePath = emailTemplateDTO.getId() + "-" + emailTemplateDTO.getLocale() + ".ftl";
        } else {
            templatePath = emailTemplateDTO.getId() + ".ftl";
        }

        Template template = freemarkerConfiguration.getTemplate(templatePath);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }
}