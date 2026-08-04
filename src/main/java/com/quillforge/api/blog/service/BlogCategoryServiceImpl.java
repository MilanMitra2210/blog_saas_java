package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogCategoryDto;
import com.quillforge.api.blog.entity.BlogCategory;
import com.quillforge.api.blog.mapper.BlogMapper;
import com.quillforge.api.blog.repository.BlogCategoryRepository;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quillforge.api.common.service.RevalidationService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BlogCategoryServiceImpl implements BlogCategoryService {

    private final RevalidationService revalidationService;

    private final BlogCategoryRepository blogCategoryRepository;
    private final BlogMapper blogMapper;

    @Override
    public PaginatedResponse<BlogCategoryDto> getCategories(int page, int limit, String search) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        Page<BlogCategory> categoryPage = blogCategoryRepository.findAllFiltered(search, pageRequest);
        return PaginatedResponse.of(categoryPage, blogMapper::toDto);
    }

    @Override
    public List<BlogCategoryDto> getAllCategories() {
        return blogCategoryRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(blogMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BlogCategoryDto createCategory(BlogCategoryDto dto) {
        if (blogCategoryRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Category with slug '" + dto.getSlug() + "' already exists");
        }
        BlogCategory cat = blogMapper.toEntity(dto);
        BlogCategory saved = blogCategoryRepository.save(cat);
        revalidationService.revalidate("blog_category", saved.getSlug(), "create");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public BlogCategoryDto updateCategory(UUID id, BlogCategoryDto dto) {
        BlogCategory cat = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        blogCategoryRepository.findBySlug(dto.getSlug()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Category with slug '" + dto.getSlug() + "' already exists");
            }
        });

        cat.setName(dto.getName());
        cat.setSlug(dto.getSlug());
        cat.setActive(dto.isActive());
        BlogCategory saved = blogCategoryRepository.save(cat);
        revalidationService.revalidate("blog_category", saved.getSlug(), "update");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        BlogCategory cat = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        cat.setDeleted(true);
        cat.setDeletedAt(Instant.now());
        BlogCategory saved = blogCategoryRepository.save(cat);
        revalidationService.revalidate("blog_category", saved.getSlug(), "delete");
    }
}
