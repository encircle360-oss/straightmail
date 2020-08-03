package com.encircle360.oss.straightmail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/templates/**")
            .addResourceLocations("file:/root/resources/templates/");

        registry
            .addResourceHandler("/i18n/**")
            .addResourceLocations("file:/root/resources/i18n/");
    }
}
