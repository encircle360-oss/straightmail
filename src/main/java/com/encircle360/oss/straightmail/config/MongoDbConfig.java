package com.encircle360.oss.straightmail.config;

import static com.encircle360.oss.straightmail.config.MongoDbConfig.PROFILE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile({PROFILE})
@RequiredArgsConstructor
@EnableConfigurationProperties(MongoProperties.class)
@EnableMongoRepositories("com.encircle360.oss.straightmail.repository")
public class MongoDbConfig extends AbstractMongoClientConfiguration {

    private final MongoProperties properties;

    public final static String PROFILE = "mongo";

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        String uri = properties.getUri();
        if (uri == null) {
            uri = "mongodb://" + properties.getHost() + ":" + properties.getPort() + "/" + properties.getDatabase();
        }

        ConnectionString connectionString = new ConnectionString(uri);
        builder.applyConnectionString(connectionString);

        if (properties.getUsername() != null && properties.getAuthenticationDatabase() != null && properties.getPassword() != null) {
            MongoCredential credentials = MongoCredential.createCredential(properties.getUsername(), properties.getAuthenticationDatabase(), properties.getPassword());
            builder.credential(credentials);
        }
    }

}
