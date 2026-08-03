package com.quillforge.api.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SitemapDataDto {

    private List<SitemapItem> blogs;
    private List<SitemapItem> categories;
    private List<SitemapItem> cmsPages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SitemapItem {
        private String slug;
        private Instant lastModified;
    }
}
