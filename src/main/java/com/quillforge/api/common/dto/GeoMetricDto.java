package com.quillforge.api.common.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class GeoMetricDto {
    private UUID id;
    private UUID entityId;
    private String entityType;
    private String countryCode;
    private int views;
    private int uniqueViews;
}
