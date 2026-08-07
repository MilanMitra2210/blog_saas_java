package com.quillforge.api.common.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.UtmCampaignMetricDto;
import com.quillforge.api.common.entity.UtmCampaignMetric;
import com.quillforge.api.common.repository.UtmCampaignMetricRepository;
import com.quillforge.api.common.dto.EngagementMilestoneDto;
import com.quillforge.api.common.entity.EngagementMilestone;
import com.quillforge.api.common.entity.AnalyticsMetric;
import com.quillforge.api.common.repository.EngagementMilestoneRepository;
import com.quillforge.api.common.repository.AnalyticsMetricRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/analytics")
@Tag(name = "Admin Analytics", description = "Admin endpoints for analyzing site metrics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final UtmCampaignMetricRepository utmCampaignMetricRepository;
    private final EngagementMilestoneRepository engagementMilestoneRepository;
    private final AnalyticsMetricRepository analyticsMetricRepository;

    @GetMapping("/utm")
    @Operation(summary = "Get UTM campaign analytics for a page or blog post")
    public ResponseEntity<ApiResponse<List<UtmCampaignMetricDto>>> getUtmMetrics(
            @RequestParam UUID entityId,
            @RequestParam String entityType
    ) {
        List<UtmCampaignMetric> metrics = utmCampaignMetricRepository.findByEntityIdAndEntityType(entityId, entityType);
        
        List<UtmCampaignMetricDto> dtos = metrics.stream().map(m -> {
            UtmCampaignMetricDto dto = new UtmCampaignMetricDto();
            dto.setId(m.getId());
            dto.setUtmSource(m.getUtmSource());
            dto.setUtmMedium(m.getUtmMedium());
            dto.setUtmCampaign(m.getUtmCampaign());
            dto.setViews(m.getViews());
            dto.setUniqueViews(m.getUniqueViews());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("UTM campaign analytics retrieved successfully", dtos));
    }

    @GetMapping("/engagement")
    @Operation(summary = "Get engagement milestones for a page or blog post")
    public ResponseEntity<ApiResponse<EngagementMilestoneDto>> getEngagementMetrics(
            @RequestParam UUID entityId,
            @RequestParam String entityType
    ) {
        String normalizedType = "blog".equalsIgnoreCase(entityType) ? "BLOG" : "CMS";
        EngagementMilestone milestone = engagementMilestoneRepository.findByEntityIdAndEntityType(entityId, normalizedType)
                .orElseGet(() -> {
                    EngagementMilestone em = new EngagementMilestone();
                    em.setEntityId(entityId);
                    em.setEntityType(normalizedType);
                    return em;
                });

        AnalyticsMetric metric = analyticsMetricRepository.findByEntityIdAndEntityType(entityId, "blog".equalsIgnoreCase(entityType) ? "blog" : "cms_page")
                .orElse(null);
        int totalViews = metric != null ? metric.getViews() : 0;

        EngagementMilestoneDto dto = new EngagementMilestoneDto();
        dto.setId(milestone.getId());
        dto.setEntityId(milestone.getEntityId());
        dto.setEntityType(milestone.getEntityType());
        dto.setMilestone25(milestone.getMilestone25());
        dto.setMilestone50(milestone.getMilestone50());
        dto.setMilestone75(milestone.getMilestone75());
        dto.setMilestone100(milestone.getMilestone100());
        dto.setTotalViews(totalViews);

        return ResponseEntity.ok(ApiResponse.success("Engagement milestones retrieved successfully", dto));
    }
}
