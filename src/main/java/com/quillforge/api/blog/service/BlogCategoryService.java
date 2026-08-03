package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogCategoryDto;
import com.quillforge.api.common.dto.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface BlogCategoryService {
    PaginatedResponse<BlogCategoryDto> getCategories(int page, int limit, String search);
    List<BlogCategoryDto> getAllCategories();
    BlogCategoryDto createCategory(BlogCategoryDto dto);
    BlogCategoryDto updateCategory(UUID id, BlogCategoryDto dto);
    void deleteCategory(UUID id);
}
