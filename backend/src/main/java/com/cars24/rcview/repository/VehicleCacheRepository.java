package com.cars24.rcview.repository;

import com.cars24.rcview.entity.VehicleCache;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface VehicleCacheRepository extends MongoRepository<VehicleCache, String> {

    Optional<VehicleCache> findByRegNoNormalizedAndExpiresAtAfter(String regNoNormalized, Instant now);
}
