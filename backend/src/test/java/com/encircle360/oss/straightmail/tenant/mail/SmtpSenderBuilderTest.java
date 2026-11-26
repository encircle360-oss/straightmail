package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SmtpSenderBuilderTest {

    @Mock
    private EncryptionService encryptionService;

    private static final SmtpSenderBuilder.SmtpConfig GLOBAL =
            new SmtpSenderBuilder.SmtpConfig("localhost", 1025, "", "", false, false);

    @Test
    void build_usesGlobalFallback_whenTenantHostIsNull() {
        SmtpSenderBuilder.SmtpConfig tenant =
                new SmtpSenderBuilder.SmtpConfig(null, null, null, null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) SmtpSenderBuilder.build(tenant, GLOBAL, encryptionService, "test");

        assertEquals("localhost", sender.getHost());
        assertEquals(1025, sender.getPort());
    }

    @Test
    void build_tenantHostOverridesGlobal() {
        SmtpSenderBuilder.SmtpConfig tenant =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 587, "user@acme.com", null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) SmtpSenderBuilder.build(tenant, GLOBAL, encryptionService, "test");

        assertEquals("smtp.acme.com", sender.getHost());
        assertEquals(587, sender.getPort());
        assertEquals("user@acme.com", sender.getUsername());
    }

    @Test
    void port465_autoEnablesSsl_regardlessOfFlag() {
        SmtpSenderBuilder.SmtpConfig tenant =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 465, null, null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        Properties props = propsFor(tenant);

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_tlsOnly_enablesStarttls() {
        SmtpSenderBuilder.SmtpConfig tenant =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 587, null, null, false, true);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        Properties props = propsFor(tenant);

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("true", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_sslTakesPrecedenceOverTls() {
        SmtpSenderBuilder.SmtpConfig tenant =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 587, null, null, true, true);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        Properties props = propsFor(tenant);

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void noTenantSmtp_globalTlsApplied() {
        // Tenant has no SMTP host → global TLS flags are used
        SmtpSenderBuilder.SmtpConfig tenantNoSmtp =
                new SmtpSenderBuilder.SmtpConfig(null, null, null, null, false, false);
        SmtpSenderBuilder.SmtpConfig globalWithTls =
                new SmtpSenderBuilder.SmtpConfig("localhost", 587, "", "", false, true);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        Properties props = ((JavaMailSenderImpl) SmtpSenderBuilder.build(
                tenantNoSmtp, globalWithTls, encryptionService, "test")).getJavaMailProperties();

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("true", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void tenantWithOwnHost_neverInheritsGlobalCredentials() {
        // A tenant relay is tenant-controlled: leaking the global username and password to it
        // would hand over the credentials of every other tenant's mail path.
        SmtpSenderBuilder.SmtpConfig globalWithCredentials =
                new SmtpSenderBuilder.SmtpConfig("smtp.internal", 25, "global-user", "global-pass", false, false);
        SmtpSenderBuilder.SmtpConfig tenantHostOnly =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", null, null, null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) SmtpSenderBuilder.build(
                tenantHostOnly, globalWithCredentials, encryptionService, "test");

        assertEquals("smtp.acme.com", sender.getHost());
        assertNull(sender.getUsername());
        assertNull(sender.getPassword());
    }

    @Test
    void withoutAUsername_smtpAuthStaysOff() {
        // JavaMail refuses to connect at all when auth is announced but no credentials exist,
        // which broke sending to credential-less relays such as Mailpit.
        SmtpSenderBuilder.SmtpConfig tenantNoCredentials =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 25, null, null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        Properties props = propsFor(tenantNoCredentials);

        assertEquals("false", props.get("mail.smtp.auth"));
    }

    @Test
    void withAUsername_smtpAuthIsAnnounced() {
        SmtpSenderBuilder.SmtpConfig tenantWithUser =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", 587, "user@acme.com", "enc", false, true);

        lenient().when(encryptionService.decrypt("enc")).thenReturn("secret");

        Properties props = propsFor(tenantWithUser);

        assertEquals("true", props.get("mail.smtp.auth"));
    }

    @Test
    void tenantWithoutPort_keepsJavaMailDefault() {
        SmtpSenderBuilder.SmtpConfig tenantNoPort =
                new SmtpSenderBuilder.SmtpConfig("smtp.acme.com", null, null, null, false, false);

        lenient().when(encryptionService.decrypt(isNull())).thenReturn(null);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) SmtpSenderBuilder.build(
                tenantNoPort, GLOBAL, encryptionService, "test");

        assertEquals(JavaMailSenderImpl.DEFAULT_PORT, sender.getPort());
    }

    private Properties propsFor(SmtpSenderBuilder.SmtpConfig tenant) {
        JavaMailSender sender = SmtpSenderBuilder.build(tenant, GLOBAL, encryptionService, "test");
        return ((JavaMailSenderImpl) sender).getJavaMailProperties();
    }
}
