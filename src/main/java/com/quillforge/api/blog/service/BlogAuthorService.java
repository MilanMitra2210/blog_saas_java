package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogAuthorDto;
import com.quillforge.api.common.dto.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface BlogAuthorService {
    PaginatedResponse<BlogAuthorDto> getAuthors(int page, int limit, String search);
    List<BlogAuthorDto> getAllAuthors();
    BlogAuthorDto createAuthor(BlogAuthorDto dto);
    BlogAuthorDto updateAuthor(UUID id, BlogAuthorDto dto);
    void deleteAuthor(UUID id);
}
