package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.service.template.provider.FileTemplateSourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class StartupConfigLoggerTest {

    private static final String ENCRYPTION_KEY = "s3cr3tEncryptionKeyValue";
    private static final String API_KEY = "apik3yValue";

    @SuppressWarnings("unchecked")
    private StartupConfigLogger loggerWith(String encryptionKey, String apiKey) {
        StartupConfigLogger logger = new StartupConfigLogger(mock(ObjectProvider.class));
        ReflectionTestUtils.setField(logger, "corsAllowedOrigins", "http://localhost:4200");
        ReflectionTestUtils.setField(logger, "encryptionKey", encryptionKey);
        ReflectionTestUtils.setField(logger, "jwtTenantClaim", "tenant_id");
        ReflectionTestUtils.setField(logger, "jwtTenantIdsClaim", "tenant_ids");
        ReflectionTestUtils.setField(logger, "authMode", "api-key");
        ReflectionTestUtils.setField(logger, "authIssuerUri", "");
        ReflectionTestUtils.setField(logger, "apiPrefix", "/api");
        ReflectionTestUtils.setField(logger, "apiKey", apiKey);
        ReflectionTestUtils.setField(logger, "gitSyncBasePath", "/git");
        ReflectionTestUtils.setField(logger, "gitSyncEnabled", false);
        ReflectionTestUtils.setField(logger, "gitSyncCron", "0 0 * * * *");
        ReflectionTestUtils.setField(logger, "gitSyncLockMin", "PT1M");
        ReflectionTestUtils.setField(logger, "gitSyncLockMax", "PT2M");
        ReflectionTestUtils.setField(logger, "fileEnabled", false);
        ReflectionTestUtils.setField(logger, "fileBasePath", "/resources/templates");
        return logger;
    }

    @Test
    void secretsAreReportedOnlyAsSet(CapturedOutput output) {
        loggerWith(ENCRYPTION_KEY, API_KEY).logConfig();

        assertThat(output.getOut()).containsPattern("Encryption key\\s+: \\(set\\)");
        assertThat(output.getOut()).containsPattern("API key\\s+: \\(set\\)");
        assertThat(output.getOut())
                .doesNotContain(ENCRYPTION_KEY.substring(0, 4))
                .doesNotContain(API_KEY.substring(0, 4));
    }

    @Test
    void blankSecretsAreReportedAsNotSet(CapturedOutput output) {
        loggerWith("", null).logConfig();

        assertThat(output.getOut()).containsPattern("Encryption key\\s+: \\(not set\\)");
        assertThat(output.getOut()).containsPattern("API key\\s+: \\(not set\\)");
    }
}
