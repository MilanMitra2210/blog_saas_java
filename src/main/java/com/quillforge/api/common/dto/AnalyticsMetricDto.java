package com.quillforge.api.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AnalyticsMetricDto {
    private UUID id;
    private UUID entityId;
    private String entityType;
    private int views;
    private int likes;

    @JsonProperty("readProgressCount")
    private int readProgressCount;

    private int googleViews;
    private int twitterViews;
    private int linkedinViews;
    private int directViews;
    private int otherViews;
    private int uniqueViews;
    private int mobileViews;
    private int tabletViews;
    private int desktopViews;

    public double getCompletionRate() {
        return views > 0 ? Math.round((double) readProgressCount / views * 100.0 * 10.0) / 10.0 : 0.0;
    }
}
