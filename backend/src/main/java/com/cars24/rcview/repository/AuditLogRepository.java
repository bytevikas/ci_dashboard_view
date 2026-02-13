package com.cars24.rcview.repository;

import com.cars24.rcview.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    long countByUserIdAndActionInAndCreatedAtAfter(String userId, Collection<AuditLog.AuditAction> actions, Instant since);
}
