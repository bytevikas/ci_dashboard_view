package com.cars24.rcview.repository;

import com.cars24.rcview.entity.AppConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppConfigRepository extends MongoRepository<AppConfig, String> {
}
