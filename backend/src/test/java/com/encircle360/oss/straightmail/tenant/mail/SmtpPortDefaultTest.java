package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * Starts the SMTP configuration from the shipped {@code application.yml} with no environment
 * variables, so an unset {@code SMTP_PORT} falls back to the default port.
 *
 * <p>The file is read from the source tree because the test {@code application.yml} shadows the
 * main one on the test classpath.
 */
class SmtpPortDefaultTest {

    private static final Resource MAIN_APPLICATION_YML = new FileSystemResource("src/main/resources/application.yml");

    @Configuration
    @EnableConfigurationProperties(MailProperties.class)
    static class MailPropertiesConfig {
    }

    @Test
    void unsetSmtpPortDefaultsTo587() {
        new ApplicationContextRunner()
                .withInitializer(context -> {
                    MutablePropertySources sources = context.getEnvironment().getPropertySources();
                    sources.remove(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    try {
                        new YamlPropertySourceLoader().load("application.yml", MAIN_APPLICATION_YML)
                                .forEach(sources::addLast);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .withBean(EncryptionService.class, () -> mock(EncryptionService.class))
                .withUserConfiguration(MailPropertiesConfig.class, ConfigBasedMailSenderFactory.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MailProperties.class).getPort()).isEqualTo(587);
                    assertThat(ReflectionTestUtils.getField(
                            context.getBean(ConfigBasedMailSenderFactory.class), "globalPort")).isEqualTo(587);
                });
    }
}
