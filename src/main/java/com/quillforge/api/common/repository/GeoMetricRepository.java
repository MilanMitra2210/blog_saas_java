package com.quillforge.api.common.repository;

import com.quillforge.api.common.entity.GeoMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoMetricRepository extends JpaRepository<GeoMetric, UUID> {
    List<GeoMetric> findByEntityIdAndEntityType(UUID entityId, String entityType);
    Optional<GeoMetric> findByEntityIdAndEntityTypeAndCountryCode(UUID entityId, String entityType, String countryCode);
}
