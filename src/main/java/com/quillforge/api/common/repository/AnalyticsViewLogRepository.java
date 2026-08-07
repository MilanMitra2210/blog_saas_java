package com.quillforge.api.common.repository;

import com.quillforge.api.common.entity.AnalyticsViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsViewLogRepository extends JpaRepository<AnalyticsViewLog, UUID> {
    boolean existsByEntityIdAndEntityTypeAndIpHash(UUID entityId, String entityType, String ipHash);
}
