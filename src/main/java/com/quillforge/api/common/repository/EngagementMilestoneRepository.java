package com.quillforge.api.common.repository;

import com.quillforge.api.common.entity.EngagementMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EngagementMilestoneRepository extends JpaRepository<EngagementMilestone, UUID> {
    Optional<EngagementMilestone> findByEntityIdAndEntityType(UUID entityId, String entityType);

    @Modifying
    @Query("UPDATE EngagementMilestone e SET e.milestone25 = e.milestone25 + 1 WHERE e.entityId = :entityId AND e.entityType = :entityType")
    int incrementMilestone25(@Param("entityId") UUID entityId, @Param("entityType") String entityType);

    @Modifying
    @Query("UPDATE EngagementMilestone e SET e.milestone50 = e.milestone50 + 1 WHERE e.entityId = :entityId AND e.entityType = :entityType")
    int incrementMilestone50(@Param("entityId") UUID entityId, @Param("entityType") String entityType);

    @Modifying
    @Query("UPDATE EngagementMilestone e SET e.milestone75 = e.milestone75 + 1 WHERE e.entityId = :entityId AND e.entityType = :entityType")
    int incrementMilestone75(@Param("entityId") UUID entityId, @Param("entityType") String entityType);

    @Modifying
    @Query("UPDATE EngagementMilestone e SET e.milestone100 = e.milestone100 + 1 WHERE e.entityId = :entityId AND e.entityType = :entityType")
    int incrementMilestone100(@Param("entityId") UUID entityId, @Param("entityType") String entityType);
}
