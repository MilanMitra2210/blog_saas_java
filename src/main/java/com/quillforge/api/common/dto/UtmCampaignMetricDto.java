package com.quillforge.api.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UtmCampaignMetricDto {
    private UUID id;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private int views;
    private int uniqueViews;
}
