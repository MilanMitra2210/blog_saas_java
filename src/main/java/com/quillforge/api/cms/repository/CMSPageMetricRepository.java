package com.quillforge.api.cms.repository;

import com.quillforge.api.cms.entity.CMSPageMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CMSPageMetricRepository extends JpaRepository<CMSPageMetric, UUID> {
    Optional<CMSPageMetric> findByPageId(UUID pageId);
}
