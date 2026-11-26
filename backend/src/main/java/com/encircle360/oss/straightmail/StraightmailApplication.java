package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static freemarker.template.Configuration.VERSION_2_3_28;

@Slf4j
@SpringBootApplication
public class StraightmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(StraightmailApplication.class).start();
    }

    @Bean
    JsonNodeObjectWrapper jsonNodeObjectWrapper() {
        return new JsonNodeObjectWrapper(VERSION_2_3_28);
    }

}
