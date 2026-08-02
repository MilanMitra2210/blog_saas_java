package com.quillforge.api.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Reusable DTO representing a content block.
 */
@Getter
@Setter
public class ContentBlockDto {

    @JsonProperty("id")
    private String blockKey;

    private Map<String, Object> content;
}
