package com.encircle360.oss.straightmail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * Message bundles available to templates through {@code <@spring.message 'key' />}.
 *
 * <p>Bundles ({@code messages.properties}, {@code messages_de.properties}, …) in the directory
 * configured by {@code i18n.base-path} (default {@code /resources/i18n}) are read first; the
 * built-in bundles under {@code classpath:i18n/} answer every key the external directory does not
 * define. A missing directory or file is skipped. Files are read as UTF-8.
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Creates the application {@link MessageSource} over the external and the built-in bundles.
     *
     * @param basePath directory that holds the external {@code messages*.properties} files
     * @return the message source used by Spring and the FreeMarker {@code spring.message} macro
     */
    @Bean
    public MessageSource messageSource(@Value("${i18n.base-path:/resources/i18n}") String basePath) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(
                "file:" + basePath.replaceAll("/+$", "") + "/messages",
                "classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return messageSource;
    }
}
