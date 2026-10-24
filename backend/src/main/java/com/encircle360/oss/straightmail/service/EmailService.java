package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.email.*;
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.loader.TemplateLoader;
import com.encircle360.oss.straightmail.service.template.provider.CompositeTemplateProvider;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import com.encircle360.oss.straightmail.tenant.mail.ConfigBasedMailSenderFactory;
import com.encircle360.oss.straightmail.tenant.mail.TenantMailSenderFactory;
import com.encircle360.oss.straightmail.util.HtmlUtil;
import com.encircle360.oss.straightmail.util.LogSanitizer;
import tools.jackson.databind.JsonNode;
import freemarker.template.TemplateException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

/**
 * Core service responsible for composing and dispatching email messages.
 *
 * <p>Supports two operating modes depending on active Spring profiles:
 * <ul>
 *   <li><b>File-based mode</b> (no {@code database} profile): resolves templates via {@link TemplateLoader}
 *       and uses per-tenant SMTP configuration from {@link ConfigBasedMailSenderFactory} (falling back to
 *       the global {@link JavaMailSender} configured in {@code application.yml}).</li>
 *   <li><b>Database mode</b> ({@code database} profile active): resolves templates via
 *       {@link CompositeTemplateProvider}, uses per-tenant SMTP configuration from
 *       {@link TenantMailSenderFactory}, and reads the current tenant from {@link TenantContext}.</li>
 * </ul>
 *
 * <p>In both modes the tenant's configured {@code smtpSender} is authoritative: it always overrides
 * any {@code sender} supplied in the request. If no tenant sender is configured the request sender
 * is used, with the global {@code spring.mail.default-sender} as the last fallback.
 *
 * <p>Profile-restricted optional dependencies are injected via {@link ObjectProvider} so that the
 * service starts correctly regardless of which profile is active.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.default-sender:}")
    private String globalDefaultSender;

    private final FreemarkerService freemarkerService;
    private final Base64.Decoder decoder = Base64.getDecoder();

    // Always-available: per-tenant SMTP from static config (non-DB mode primary, DB mode fallback)
    private final TenantProperties tenantProperties;
    private final ConfigBasedMailSenderFactory configMailSenderFactory;

    // Non-DB fallback (used when database profile is not active)
    private final ObjectProvider<TemplateLoader> templateLoader;

    // Global fallback mail sender (last resort when no tenant config is found)
    private final ObjectProvider<JavaMailSender> emailClient;

    // DB-mode multi-tenant dependencies (optional — only present with database profile)
    private final ObjectProvider<CompositeTemplateProvider> compositeTemplateProvider;
    private final ObjectProvider<TenantContext> tenantContext;
    private final ObjectProvider<TenantService> tenantService;
    private final ObjectProvider<TenantMailSenderFactory> mailSenderFactory;

    /**
     * Sends an email based on the given request.
     *
     * <p>Accepts either an {@link EmailTemplateFileRequestDTO} (resolved by template ID) or an
     * {@link EmailInlineTemplateRequestDTO} (with inline subject, body, and plain-text templates).
     * Templates are rendered via FreeMarker before being dispatched.
     *
     * @param <T>          the concrete email request type
     * @param emailRequest the email request containing recipients, template reference, and model data
     * @return an {@link EmailResultDTO} indicating success or failure with an explanatory message;
     * if {@code verbose} is set on the request, a {@link DetailedEmailResultDTO} with the
     * rendered content is returned instead
     */
    public <T extends EmailRequestDTO> EmailResultDTO sendMail(T emailRequest) {
        if (emailRequest == null) {
            return this.result("Request was empty");
        }

        String locale = emailRequest.getLocale();
        HashMap<String, JsonNode> model = emailRequest.getModel();
        String templateId = null;
        String templateName = null;
        String templateSource = "unknown";

        String plainTextTemplate = null;
        String bodyTemplate = null;
        String subjectTemplate = null;

        if (emailRequest instanceof EmailTemplateFileRequestDTO emailTemplateFileRequest) {
            templateSource = "file";
            templateId = emailTemplateFileRequest.getEmailTemplateId();
            Template template = this.resolveTemplate(templateId);
            if (template == null) {
                return this.result("Template not found");
            }
            templateName = template.getName();
            subjectTemplate = template.getSubject();
            bodyTemplate = template.getHtml();
            plainTextTemplate = template.getPlain();
        } else if (emailRequest instanceof EmailInlineTemplateRequestDTO inlineTemplateRequest) {
            templateSource = "inline";
            subjectTemplate = inlineTemplateRequest.getSubject();
            bodyTemplate = inlineTemplateRequest.getEmailTemplate();
            plainTextTemplate = inlineTemplateRequest.getPlainText();
        }

        // Operator visibility: request shape only — no recipient values, no rendered content.
        // Recipient addresses and rendered template bodies can carry personal data.
        log.info("Mail request received: source={}, templateId={}, recipients={}, cc={}, bcc={}, attachments={}, locale={}",
                templateSource, LogSanitizer.sanitize(templateId),
                size(emailRequest.getRecipients()), size(emailRequest.getCc()),
                size(emailRequest.getBcc()), size(emailRequest.getAttachments()),
                LogSanitizer.sanitize(locale));

        String[] rendered = this.renderTemplates(subjectTemplate, bodyTemplate, plainTextTemplate, locale, model);

        String subject = rendered[0];
        String body = rendered[1];
        String plainText = rendered[2];

        if (subject == null || body == null) {
            log.error("Couldn't render template (templateId={}): subject={} body={}",
                    LogSanitizer.sanitize(templateId), subject != null, body != null);
            return this.result("Error while rendering template");
        }

        JavaMailSender sender = this.resolveSender();

        String tenantSender = this.resolveTenantSender();
        if (tenantSender != null && !tenantSender.isBlank()) {
            // Tenant-configured sender is always authoritative
            emailRequest.setSender(tenantSender);
        } else if (emailRequest.getSender() == null) {
            // No tenant sender configured — use request sender or fall back to global default
            emailRequest.setSender(globalDefaultSender);
        }

        MimeMessage message = this.buildAndSend(emailRequest, subject, body, plainText, sender);
        if (message == null) {
            return this.result("Error creating mimetype message, maybe some missing or invalid fields");
        }

        sender.send(message);

        log.info("Mail dispatched to SMTP: source={}, templateId={}, recipients={}",
                templateSource, LogSanitizer.sanitize(templateId), size(emailRequest.getRecipients()));

        RenderedTemplateDTO renderedTemplateDTO = null;
        if (emailRequest.isVerbose()) {
            renderedTemplateDTO = RenderedTemplateDTO.builder()
                    .html(body)
                    .plain(plainText)
                    .name(templateName)
                    .id(templateId)
                    .build();
        }

        return this.result("Message was send to SMTP Server", renderedTemplateDTO, subject, true);
    }

    private Template resolveTemplate(String templateId) {
        if (templateId == null) return null;
        // Decode :: → / so that encoded file-path IDs (e.g. "templates::default") resolve correctly
        String name = templateId.replace("::", "/");

        CompositeTemplateProvider provider = compositeTemplateProvider.getIfAvailable();
        TenantContext ctx = tenantContext.getIfAvailable();
        if (provider != null) {
            String tenantId = ctx != null ? ctx.getTenantId() : null;
            Optional<Template> resolved = provider.resolve(tenantId, name);
            if (resolved.isPresent()) return resolved.get();
        }

        TemplateLoader loader = templateLoader.getIfAvailable();
        if (loader != null) {
            return loader.loadTemplate(name);
        }

        return null;
    }

    private String[] renderTemplates(String subjectTemplate, String bodyTemplate, String plainTextTemplate,
                                     String locale, HashMap<String, JsonNode> model) {
        try {
            String subject = freemarkerService.renderTemplateToString(subjectTemplate, locale, model);
            String body = freemarkerService.renderTemplateToString(bodyTemplate, locale, model);
            String plain = freemarkerService.renderTemplateToString(plainTextTemplate, locale, model);
            return new String[]{subject, body, plain};
        } catch (IOException | TemplateException e) {
            log.error("Error while rendering template to string.", e);
            // Three null slots, not an empty array: sendMail() reads rendered[0..2] and
            // reports the failure through its null guard as a 400, not as an index crash.
            return new String[]{null, null, null};
        }
    }

    private JavaMailSender resolveSender() {
        TenantContext ctx = tenantContext.getIfAvailable();

        // Database mode: per-tenant SMTP from DB entity
        TenantMailSenderFactory factory = mailSenderFactory.getIfAvailable();
        TenantService service = tenantService.getIfAvailable();
        if (factory != null && ctx != null && service != null) {
            try {
                return factory.forTenant(service.get(ctx.getTenantId()));
            } catch (Exception e) {
                log.warn("Could not resolve DB tenant mail sender, falling back to config-based", e);
            }
        }

        // Non-database mode (or DB fallback): per-tenant SMTP from TenantConfig (application.yml)
        if (ctx != null) {
            var configSender = tenantProperties.getConfig().stream()
                    .filter(c -> c.getId().equals(ctx.getTenantId()))
                    .findFirst()
                    .map(configMailSenderFactory::forTenant);
            if (configSender.isPresent()) {
                return configSender.get();
            }
        }

        return emailClient.getIfAvailable();
    }

    private String resolveTenantSender() {
        TenantContext ctx = tenantContext.getIfAvailable();
        if (ctx == null) return null;

        // Database mode: read from DB entity
        TenantService service = tenantService.getIfAvailable();
        if (service != null) {
            try {
                String tenantSender = service.get(ctx.getTenantId()).getSmtpSender();
                if (tenantSender != null && !tenantSender.isBlank()) {
                    return tenantSender;
                }
            } catch (Exception e) {
                log.debug("Could not resolve tenant sender from DB, trying config");
            }
        }

        // Non-database mode (or DB fallback): read from TenantConfig (application.yml)
        return tenantProperties.getConfig().stream()
                .filter(c -> c.getId().equals(ctx.getTenantId()))
                .findFirst()
                .map(TenantProperties.TenantConfig::getSmtpSender)
                .filter(s -> !s.isBlank())
                .orElse(null);
    }

    private MimeMessage buildAndSend(EmailRequestDTO emailRequest, String subject, String htmlBody,
                                     String plainText, JavaMailSender sender) {
        MimeMessage message = sender.createMimeMessage();

        if (subject == null || htmlBody == null) {
            return null;
        }

        subject = Jsoup.clean(subject, Safelist.none());

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            if (plainText == null) {
                plainText = Jsoup.clean(htmlBody,
                        Safelist.none().addTags("br", "a").addAttributes("a", "href"));
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

    private static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private EmailResultDTO result(String message) {
        return this.result(message, null, null, false);
    }

    private EmailResultDTO result(String message, RenderedTemplateDTO renderedTemplate,
                                  String subject, boolean success) {
        if (renderedTemplate != null) {
            return DetailedEmailResultDTO.builder()
                    .message(message)
                    .success(success)
                    .renderResult(renderedTemplate)
                    .subject(subject)
                    .build();
        }

        return EmailResultDTO.builder()
                .message(message)
                .success(success)
                .build();
    }
}
