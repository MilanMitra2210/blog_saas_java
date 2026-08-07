package com.quillforge.api.common.repository;

import com.quillforge.api.common.entity.AnalyticsMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetric, UUID> {
    Optional<AnalyticsMetric> findByEntityIdAndEntityType(UUID entityId, String entityType);
}
