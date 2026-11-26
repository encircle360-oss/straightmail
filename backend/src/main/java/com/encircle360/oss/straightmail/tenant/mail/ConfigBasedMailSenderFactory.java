package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that creates and caches {@link JavaMailSender} instances from statically configured
 * {@link TenantProperties.TenantConfig} entries (i.e. tenants declared in {@code application.yml}).
 *
 * <p>Active in all Spring profiles (no {@code @Profile} restriction). This factory is the
 * counterpart to {@link TenantMailSenderFactory}, which serves the same purpose when the
 * {@code database} profile is active. In non-database mode this factory is the primary
 * per-tenant SMTP resolver; in database mode it acts as a fallback when the DB-based
 * factory cannot resolve a sender.
 *
 * <p>For every field not explicitly set on a {@link TenantProperties.TenantConfig}, the factory
 * falls back to the global {@code spring.mail.*} settings as a whole, mirroring the
 * behaviour of {@link TenantMailSenderFactory}.
 *
 * <p>Passwords in {@code TenantConfig} are supplied as plain text from YAML or environment
 * variables. {@link EncryptionService#decrypt(String)} is still invoked so that callers may
 * optionally store {@code ENC(...)} wrapped values in config without additional ceremony.
 *
 * <p>Senders are cached in a {@link ConcurrentHashMap} keyed by {@link TenantProperties.TenantConfig#getId()}.
 * Because static configuration only changes on application restart, no explicit cache invalidation
 * is required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigBasedMailSenderFactory {

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

    private final Map<String, JavaMailSender> cache = new ConcurrentHashMap<>();

    /**
     * Returns a {@link JavaMailSender} configured for the given tenant config.
     *
     * <p>Returns the cached sender if one exists for the tenant, otherwise builds and caches a new one.
     * A tenant that declares an SMTP host is used in full; otherwise the global SMTP settings are
     * used in full. Credentials are never mixed across the two.
     *
     * @param config the tenant config whose SMTP settings should be used
     * @return a configured {@link JavaMailSender} for the tenant
     */
    public JavaMailSender forTenant(TenantProperties.TenantConfig config) {
        return cache.computeIfAbsent(config.getId(), k -> {
            log.debug("Building JavaMailSender for config-based tenant '{}'", config.getId());
            return SmtpSenderBuilder.build(
                    new SmtpSenderBuilder.SmtpConfig(
                            config.getSmtpHost(), config.getSmtpPort(),
                            config.getSmtpUser(), config.getSmtpPassword(),
                            config.isSmtpSsl(), config.isSmtpTls()),
                    new SmtpSenderBuilder.SmtpConfig(
                            globalHost, globalPort,
                            globalUser, globalPassword,
                            globalSsl, globalTls),
                    encryptionService,
                    "config-based tenant '" + config.getId() + "'");
        });
    }
}
