package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConfigBasedMailSenderFactoryTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private ConfigBasedMailSenderFactory factory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factory, "globalHost", "localhost");
        ReflectionTestUtils.setField(factory, "globalPort", 1025);
        ReflectionTestUtils.setField(factory, "globalUser", "");
        ReflectionTestUtils.setField(factory, "globalPassword", "");
        ReflectionTestUtils.setField(factory, "globalTls", false);
        ReflectionTestUtils.setField(factory, "globalSsl", false);

        lenient().when(encryptionService.decrypt(null)).thenReturn(null);
    }

    private Properties propsFor(TenantProperties.TenantConfig config) {
        JavaMailSender sender = factory.forTenant(config);
        return ((JavaMailSenderImpl) sender).getJavaMailProperties();
    }

    private TenantProperties.TenantConfig configWith(String id, int port, boolean ssl, boolean tls) {
        TenantProperties.TenantConfig config = new TenantProperties.TenantConfig();
        config.setId(id);
        config.setSmtpHost("smtp.example.com");
        config.setSmtpPort(port);
        config.setSmtpSsl(ssl);
        config.setSmtpTls(tls);
        return config;
    }

    @Test
    void port465_withSslFalse_autoEnablesSsl() {
        Properties props = propsFor(configWith("tenant-465-no-ssl", 465, false, false));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port465_withSslTrue_keepsSSLEnabled() {
        Properties props = propsFor(configWith("tenant-465-ssl", 465, true, false));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_withTlsOnly_enablesStarttls() {
        Properties props = propsFor(configWith("tenant-587-tls", 587, false, true));

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("true", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_withBothSslAndTls_sslTakesPrecedence() {
        Properties props = propsFor(configWith("tenant-587-both", 587, true, true));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port25_withBothFalse_noEncryption() {
        Properties props = propsFor(configWith("tenant-25-plain", 25, false, false));

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void noTenantSmtp_usesGlobalSettings() {
        // Tenant with no smtpHost — should coalesce to global
        TenantProperties.TenantConfig config = new TenantProperties.TenantConfig();
        config.setId("tenant-no-smtp");
        config.setSmtpHost(null);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.forTenant(config);

        assertEquals("localhost", sender.getHost());
        assertEquals(1025, sender.getPort());
    }

    @Test
    void tenantSmtpOverridesGlobal() {
        TenantProperties.TenantConfig config = new TenantProperties.TenantConfig();
        config.setId("tenant-custom-smtp");
        config.setSmtpHost("smtp.acme.com");
        config.setSmtpPort(587);
        config.setSmtpUser("user@acme.com");

        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.forTenant(config);

        assertEquals("smtp.acme.com", sender.getHost());
        assertEquals(587, sender.getPort());
        assertEquals("user@acme.com", sender.getUsername());
    }

    @Test
    void sameConfigIdReturnsCachedInstance() {
        TenantProperties.TenantConfig config = configWith("tenant-cached", 587, false, true);

        JavaMailSender first = factory.forTenant(config);
        JavaMailSender second = factory.forTenant(config);

        assertSame(first, second);
    }
}
