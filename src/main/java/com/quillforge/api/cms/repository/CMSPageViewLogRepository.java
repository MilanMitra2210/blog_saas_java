package com.quillforge.api.cms.repository;

import com.quillforge.api.cms.entity.CMSPageViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CMSPageViewLogRepository extends JpaRepository<CMSPageViewLog, UUID> {
    boolean existsByPageIdAndIpHash(UUID pageId, String ipHash);
}
