package com.yuvaraj.fitsphere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/** Enables @CreatedDate population on documents (User, Feedback). */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
