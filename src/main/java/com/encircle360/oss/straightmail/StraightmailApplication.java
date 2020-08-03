package com.encircle360.oss.straightmail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

@SpringBootApplication
public class StraightmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(StraightmailApplication.class).start();
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        if(Files.exists(Path.of("file:/root/resources/i18n/messages"))) {
            messageSource.setBasenames("file:/root/resources/i18n/messages", "i18n/messages");
        } else {
            messageSource.setBasename("i18n/messages");
        }
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return messageSource;
    }

}
