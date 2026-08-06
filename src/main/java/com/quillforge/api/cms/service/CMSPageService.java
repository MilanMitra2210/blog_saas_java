package com.quillforge.api.cms.service;

import com.quillforge.api.cms.dto.CMSPageRequest;
import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.dto.CMSPageMetricDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CMSPageService {
    Page<CMSPageResponse> getCMSPages(String search, String status, Pageable pageable);
    CMSPageResponse createCMSPage(CMSPageRequest request);
    CMSPageResponse updateCMSPage(UUID id, CMSPageRequest request);
    CMSPageResponse getCMSPageById(UUID id);
    CMSPageResponse getCMSPageBySlug(String slug);
    void deleteCMSPage(UUID id);

    CMSPageMetricDto incrementMetricWithAnalytics(UUID pageId, String metricType, String referrer, String ipAddress, String userAgent);
    CMSPageMetricDto incrementMetric(UUID pageId, String metricType);
}
