package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.TagDto;
import com.quillforge.api.common.dto.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {
    PaginatedResponse<TagDto> getTags(int page, int limit, String search);
    List<TagDto> getAllTags();
    TagDto createTag(TagDto dto);
    TagDto updateTag(UUID id, TagDto dto);
    void deleteTag(UUID id);
}
