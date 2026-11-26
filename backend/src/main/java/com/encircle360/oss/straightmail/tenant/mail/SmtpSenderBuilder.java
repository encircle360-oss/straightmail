package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Package-private utility for building {@link JavaMailSender} instances from SMTP configuration.
 *
 * <p>Centralises the sender-construction logic shared by {@link ConfigBasedMailSenderFactory}
 * and {@link TenantMailSenderFactory}, which differ only in how they supply their per-tenant
 * SMTP settings.
 *
 * <p>Not a Spring bean; all methods are static.
 */
@Slf4j
class SmtpSenderBuilder {

    /**
     * Normalised SMTP configuration record used as input to the builder.
     *
     * <p>A blank tenant {@code host} signals that the whole global config should be used instead.
     *
     * @param host     SMTP host; blank on a tenant config selects the global config
     * @param port     SMTP port; {@code null} leaves the JavaMail default in place
     * @param user     SMTP username
     * @param password SMTP password (may be {@code ENC(…)}-wrapped)
     * @param ssl      whether implicit SSL is requested for this config
     * @param tls      whether STARTTLS is requested for this config
     */
    record SmtpConfig(String host, Integer port, String user, String password, boolean ssl, boolean tls) {
    }

    private SmtpSenderBuilder() {
    }

    /**
     * Builds and returns a fully configured {@link JavaMailSender}.
     *
     * <p>The tenant config is used in full when it declares a host, otherwise the global config is
     * used in full. Credentials are never mixed across the two, so a tenant relay can only ever see
     * the tenant's own username and password. Port 465 auto-enables SSL regardless of explicit flags.
     *
     * @param tenant the per-tenant SMTP settings
     * @param global the global SMTP fallback settings
     * @param enc    encryption service used to decrypt stored passwords
     * @param label  descriptive label used in debug log messages
     * @return a configured {@link JavaMailSender}
     */
    static JavaMailSender build(SmtpConfig tenant, SmtpConfig global, EncryptionService enc, String label) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        // A tenant host switches the whole block over. Falling back field by field would ship the
        // global relay's username and password to a host the tenant controls.
        SmtpConfig effective = hasTenantSmtp(tenant) ? tenant : global;
        applySmtpConfig(sender, effective, enc);
        applyTlsConfig(sender, effective);
        log.debug("Built SMTP sender for '{}': host={}, port={}", label, sender.getHost(), sender.getPort());
        return sender;
    }

    /**
     * Returns whether the tenant declares its own SMTP host, which makes its config authoritative.
     */
    private static boolean hasTenantSmtp(SmtpConfig tenant) {
        return tenant.host() != null && !tenant.host().isBlank();
    }

    private static void applySmtpConfig(JavaMailSenderImpl sender, SmtpConfig config, EncryptionService enc) {
        sender.setHost(config.host());
        if (config.port() != null) {
            sender.setPort(config.port());
        }
        sender.setUsername(config.user());
        sender.setPassword(enc.decrypt(config.password()));
        sender.setDefaultEncoding("UTF-8");
        sender.setProtocol("smtp");
    }

    private static void applyTlsConfig(JavaMailSenderImpl sender, SmtpConfig config) {
        boolean ssl = config.ssl();
        boolean tls = config.tls();

        // Port 465 is reserved for SMTPS and always requires SSL from the start.
        // Auto-enable SSL regardless of the configured flag to prevent [EOF] errors.
        if (sender.getPort() == 465) {
            ssl = true;
        }

        Properties props = sender.getJavaMailProperties();
        // Only announce SMTP AUTH when there is actually a username to send. With auth on and no
        // credentials, JavaMail fails the connection outright ("Authentication failed") instead of
        // connecting anonymously — which is what a relay like Mailpit, or an internal MTA that
        // authorises by network, expects.
        props.put("mail.smtp.auth", String.valueOf(config.user() != null && !config.user().isBlank()));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        // STARTTLS and implicit SSL are mutually exclusive; SSL takes precedence.
        props.put("mail.smtp.starttls.enable", String.valueOf(!ssl && tls));
    }
}
