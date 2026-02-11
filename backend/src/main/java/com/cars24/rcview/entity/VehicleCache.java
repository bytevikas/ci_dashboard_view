package com.cars24.rcview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicle_cache")
public class VehicleCache {

    @Id
    private String id;

    @Indexed
    private String regNoNormalized;

    private Map<String, Object> responseData;

    @Indexed
    private Instant cachedAt;

    private Instant expiresAt;
}
