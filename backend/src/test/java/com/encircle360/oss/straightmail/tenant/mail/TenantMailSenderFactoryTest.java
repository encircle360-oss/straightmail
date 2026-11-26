package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.model.Tenant;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TenantMailSenderFactoryTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TenantMailSenderFactory factory;

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

    private Properties propsFor(Tenant tenant) {
        JavaMailSender sender = factory.forTenant(tenant);
        return ((JavaMailSenderImpl) sender).getJavaMailProperties();
    }

    private Tenant tenantWith(int port, boolean ssl, boolean tls) {
        return Tenant.builder()
                .slug("test-" + port + "-ssl" + ssl + "-tls" + tls)
                .displayName("Test")
                .smtpHost("smtp.example.com")
                .smtpPort(port)
                .smtpSsl(ssl)
                .smtpTls(tls)
                .build();
    }

    @Test
    void port465_withSslFalse_autoEnablesSsl() {
        Properties props = propsFor(tenantWith(465, false, false));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port465_withSslTrue_keepsSSLEnabled() {
        Properties props = propsFor(tenantWith(465, true, false));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_withTlsOnly_enablesStarttls() {
        Properties props = propsFor(tenantWith(587, false, true));

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("true", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port587_withBothSslAndTls_sslTakesPrecedence() {
        Properties props = propsFor(tenantWith(587, true, true));

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }

    @Test
    void port25_withBothFalse_noEncryption() {
        Properties props = propsFor(tenantWith(25, false, false));

        assertEquals("false", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
    }
}
