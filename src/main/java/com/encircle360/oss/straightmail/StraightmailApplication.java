package com.encircle360.oss.straightmail;

import static freemarker.template.Configuration.VERSION_2_3_28;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
public class StraightmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(StraightmailApplication.class).start();
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("classpath:i18n/messages", "file:/resources/i18n/messages", "i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

        return messageSource;
    }

    @Bean
    JsonNodeObjectWrapper jsonNodeObjectWrapper() {
        return new JsonNodeObjectWrapper(VERSION_2_3_28);
    }

}
