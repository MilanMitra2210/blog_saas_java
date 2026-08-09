package com.quillforge.api.common.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.UtmCampaignMetricDto;
import com.quillforge.api.common.entity.UtmCampaignMetric;
import com.quillforge.api.common.repository.UtmCampaignMetricRepository;
import com.quillforge.api.common.dto.EngagementMilestoneDto;
import com.quillforge.api.common.dto.GeoMetricDto;
import com.quillforge.api.common.entity.EngagementMilestone;
import com.quillforge.api.common.entity.AnalyticsMetric;
import com.quillforge.api.common.repository.EngagementMilestoneRepository;
import com.quillforge.api.common.repository.AnalyticsMetricRepository;
import com.quillforge.api.common.repository.GeoMetricRepository;
import com.quillforge.api.common.entity.GeoMetric;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.quillforge.api.tenant.TenantContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import com.quillforge.api.common.service.AnalyticsBufferService;

@RestController
@RequestMapping("/admin/analytics")
@Tag(name = "Admin Analytics", description = "Admin endpoints for analyzing site metrics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final UtmCampaignMetricRepository utmCampaignMetricRepository;
    private final EngagementMilestoneRepository engagementMilestoneRepository;
    private final AnalyticsMetricRepository analyticsMetricRepository;
    private final GeoMetricRepository geoMetricRepository;
    private final AnalyticsBufferService analyticsBufferService;

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

    @GetMapping("/geo")
    @Operation(summary = "Get geographic views for a page or blog post")
    public ResponseEntity<ApiResponse<List<GeoMetricDto>>> getGeoMetrics(
            @RequestParam UUID entityId,
            @RequestParam String entityType
    ) {
        List<GeoMetric> metrics = geoMetricRepository.findByEntityIdAndEntityType(entityId, entityType);
        
        List<GeoMetricDto> dtos = metrics.stream().map(m -> {
            GeoMetricDto dto = new GeoMetricDto();
            dto.setId(m.getId());
            dto.setEntityId(m.getEntityId());
            dto.setEntityType(m.getEntityType());
            dto.setCountryCode(m.getCountryCode());
            dto.setViews(m.getViews());
            dto.setUniqueViews(m.getUniqueViews());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Geographic metrics retrieved successfully", dtos));
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

    @GetMapping("/realtime/stream")
    @Operation(summary = "Establish real-time active reader stream (SSE)")
    public SseEmitter streamRealtimeActive(
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String entityType
    ) {
        String tenantId = TenantContext.getCurrentTenant();
        SseEmitter emitter = new SseEmitter(600_000L);
                
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        executor.scheduleAtFixedRate(() -> {
            try {
                long totalActive = analyticsBufferService.getTotalActiveReaders(tenantId);
                long entityActive = 0;
                if (entityId != null && entityType != null) {
                    entityActive = analyticsBufferService.getActiveReaders(tenantId, entityType, entityId);
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("totalActive", totalActive);
                data.put("entityActive", entityActive);
                
                emitter.send(SseEmitter.event()
                        .name("active-metrics")
                        .data(data));
            } catch (Exception e) {
                executor.shutdown();
                try {
                    emitter.complete();
                } catch (Exception ex) {}
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(executor::shutdown);
        emitter.onError(e -> executor.shutdown());
        
        return emitter;
    }
}
