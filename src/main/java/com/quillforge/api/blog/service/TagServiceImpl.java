package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.TagDto;
import com.quillforge.api.blog.entity.Tag;
import com.quillforge.api.blog.mapper.BlogMapper;
import com.quillforge.api.blog.repository.TagRepository;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.common.service.RevalidationService;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final BlogMapper blogMapper;
    private final RevalidationService revalidationService;

    @Override
    public PaginatedResponse<TagDto> getTags(int page, int limit, String search) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        Page<Tag> tagPage = tagRepository.findAllFiltered(search, pageRequest);
        return PaginatedResponse.of(tagPage, blogMapper::toDto);
    }

    @Override
    public List<TagDto> getAllTags() {
        return tagRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(blogMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TagDto createTag(TagDto dto) {
        if (tagRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Tag with slug '" + dto.getSlug() + "' already exists");
        }
        Tag tag = blogMapper.toEntity(dto);
        Tag saved = tagRepository.save(tag);
        revalidationService.revalidate("blog_tag", saved.getSlug(), "create");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TagDto updateTag(UUID id, TagDto dto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));

        tagRepository.findBySlug(dto.getSlug()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Tag with slug '" + dto.getSlug() + "' already exists");
            }
        });

        tag.setName(dto.getName());
        tag.setSlug(dto.getSlug());
        Tag saved = tagRepository.save(tag);
        revalidationService.revalidate("blog_tag", saved.getSlug(), "update");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
        tag.setDeleted(true);
        tag.setDeletedAt(Instant.now());
        tagRepository.save(tag);
        revalidationService.revalidate("blog_tag", tag.getSlug(), "delete");
    }
}
