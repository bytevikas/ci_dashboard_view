package com.cars24.rcview.config;

import com.mongodb.MongoClientSettings;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Prevents startup from hanging when MongoDB is unreachable (e.g. Atlas slow/wrong URI).
 * Without this, the driver can block for 30+ seconds waiting for a server.
 */
@Configuration
public class MongoConfig {

    private static final int SERVER_SELECTION_TIMEOUT_SECONDS = 3;

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTimeoutCustomizer() {
        return builder -> builder.applyToClusterSettings(b ->
                b.serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}
