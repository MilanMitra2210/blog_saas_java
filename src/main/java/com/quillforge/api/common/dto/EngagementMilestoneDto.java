package com.quillforge.api.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EngagementMilestoneDto {
    private UUID id;
    private UUID entityId;
    private String entityType;

    @JsonProperty("milestone25")
    private int milestone25;

    @JsonProperty("milestone50")
    private int milestone50;

    @JsonProperty("milestone75")
    private int milestone75;

    @JsonProperty("milestone100")
    private int milestone100;

    private int totalViews;
}
