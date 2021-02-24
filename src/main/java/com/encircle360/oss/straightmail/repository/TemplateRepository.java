package com.encircle360.oss.straightmail.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.model.Template;

@Repository
@Profile(MongoDbConfig.PROFILE)
public interface TemplateRepository extends MongoRepository<Template, String> {
}
