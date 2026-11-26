package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

import static freemarker.template.Configuration.VERSION_2_3_28;

@Profile("!database")
@SpringBootApplication(
        scanBasePackages = "com.encircle360.oss.straightmail",
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, LiquibaseAutoConfiguration.class}
)
@ComponentScan(
        basePackages = "com.encircle360.oss.straightmail",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.encircle360\\.oss\\.straightmail\\.StraightmailApplication"
        )
)
public class TestApplication {

    @Bean
    JsonNodeObjectWrapper jsonNodeObjectWrapper() {
        return new JsonNodeObjectWrapper(VERSION_2_3_28);
    }
}
