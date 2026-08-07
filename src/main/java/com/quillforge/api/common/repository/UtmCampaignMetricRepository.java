package com.quillforge.api.common.repository;

import com.quillforge.api.common.entity.UtmCampaignMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtmCampaignMetricRepository extends JpaRepository<UtmCampaignMetric, UUID> {
    Optional<UtmCampaignMetric> findByEntityIdAndEntityTypeAndUtmSourceAndUtmMediumAndUtmCampaign(
            UUID entityId, String entityType, String utmSource, String utmMedium, String utmCampaign
    );

    List<UtmCampaignMetric> findByEntityIdAndEntityType(UUID entityId, String entityType);
}
