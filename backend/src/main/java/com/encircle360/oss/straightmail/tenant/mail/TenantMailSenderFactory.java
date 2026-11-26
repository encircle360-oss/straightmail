package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Factory that creates per-tenant {@link JavaMailSender} instances on demand.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * For each tenant, a dedicated sender is built from the tenant's SMTP configuration, falling back
 * to the global SMTP settings (from {@code application.yml}) for any field not explicitly configured
 * on the tenant.
 *
 * <p>No caching is applied intentionally: a fresh sender is constructed on every call so that
 * SMTP configuration changes take effect immediately across all pods in a clustered deployment.
 * {@link SmtpSenderBuilder} defers the actual SMTP connection until the first message is sent,
 * so the construction cost is negligible.
 *
 * <p>SMTP passwords are stored encrypted in the database; decryption is performed by
 * {@link EncryptionService} when building the sender.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class TenantMailSenderFactory {

    @Value("${spring.mail.host}")
    private String globalHost;

    @Value("${spring.mail.port}")
    private int globalPort;

    @Value("${spring.mail.username:}")
    private String globalUser;

    @Value("${spring.mail.password:}")
    private String globalPassword;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean globalTls;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean globalSsl;

    private final EncryptionService encryptionService;

    /**
     * Builds and returns a {@link JavaMailSender} configured for the given tenant.
     *
     * <p>A new sender is constructed on every invocation. A tenant that declares an SMTP host is
     * used in full; otherwise the global SMTP configuration is used in full. Credentials are never
     * mixed across the two.
     *
     * @param tenant the tenant whose SMTP configuration should be used
     * @return a configured {@link JavaMailSender} for the tenant
     */
    public JavaMailSender forTenant(Tenant tenant) {
        log.debug("Building JavaMailSender for tenant '{}'", tenant.getSlug());
        return SmtpSenderBuilder.build(
                new SmtpSenderBuilder.SmtpConfig(
                        tenant.getSmtpHost(), tenant.getSmtpPort(),
                        tenant.getSmtpUser(), tenant.getSmtpPassword(),
                        tenant.isSmtpSsl(), tenant.isSmtpTls()),
                new SmtpSenderBuilder.SmtpConfig(
                        globalHost, globalPort,
                        globalUser, globalPassword,
                        globalSsl, globalTls),
                encryptionService,
                "tenant '" + tenant.getSlug() + "'");
    }
}
