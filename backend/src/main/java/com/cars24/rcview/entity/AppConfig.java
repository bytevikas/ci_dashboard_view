package com.cars24.rcview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "app_config")
public class AppConfig {

    @Id
    private String id;

    /** Cache TTL in days. */
    private int cacheTtlDays;

    /** Max requests per second per user (burst). */
    private int rateLimitPerSecond;

    /** Default max requests per day per user. */
    private int rateLimitPerDayDefault;

    private Instant updatedAt;
    private String updatedBy;
}
