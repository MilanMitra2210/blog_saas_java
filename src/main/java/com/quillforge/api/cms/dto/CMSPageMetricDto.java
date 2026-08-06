package com.quillforge.api.cms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CMSPageMetricDto {
    private UUID id;

    @JsonProperty("pageId")
    private UUID pageId;

    private int views;

    @JsonProperty("googleViews")
    private int googleViews;

    @JsonProperty("twitterViews")
    private int twitterViews;

    @JsonProperty("linkedinViews")
    private int linkedinViews;

    @JsonProperty("directViews")
    private int directViews;

    @JsonProperty("otherViews")
    private int otherViews;

    @JsonProperty("uniqueViews")
    private int uniqueViews;

    @JsonProperty("mobileViews")
    private int mobileViews;

    @JsonProperty("tabletViews")
    private int tabletViews;

    @JsonProperty("desktopViews")
    private int desktopViews;
}
