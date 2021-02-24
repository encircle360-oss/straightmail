package com.encircle360.oss.straightmail.config;

import static com.encircle360.oss.straightmail.config.MongoDbConfig.PROFILE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile({PROFILE})
@EnableConfigurationProperties(MongoProperties.class)
@EnableMongoRepositories("com.encircle360.oss.straightmail.repository")
public class MongoDbConfig extends AbstractMongoClientConfiguration {

    public final static String PROFILE = "mongo";

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }
}
