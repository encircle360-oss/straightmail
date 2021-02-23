package com.encircle360.oss.straightmail.config;

import static com.encircle360.oss.straightmail.config.MongoDbConfig.PROFILE;

import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoClientFactory;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;

@Configuration
@Profile(PROFILE)
@EnableMongoRepositories
public class MongoDbConfig extends MongoAutoConfiguration {

    public final static String PROFILE = "mongo";

    @Override
    public MongoClient mongo(MongoProperties properties, Environment environment,
                             ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
                             ObjectProvider<MongoClientSettings> settings) {
        return new MongoClientFactory(properties, environment,
            builderCustomizers.orderedStream().collect(Collectors.toList()))
            .createMongoClient(settings.getIfAvailable());
    }
}
