package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogAuthorDto;
import com.quillforge.api.blog.entity.Blog;
import com.quillforge.api.blog.entity.BlogAuthor;
import com.quillforge.api.blog.mapper.BlogMapper;
import com.quillforge.api.blog.repository.BlogAuthorRepository;
import com.quillforge.api.blog.repository.BlogRepository;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.common.service.RevalidationService;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.repository.MediaRepository;
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
public class BlogAuthorServiceImpl implements BlogAuthorService {

    private final BlogAuthorRepository blogAuthorRepository;
    private final BlogRepository blogRepository;
    private final MediaRepository mediaRepository;
    private final BlogMapper blogMapper;
    private final RevalidationService revalidationService;

    @Override
    public PaginatedResponse<BlogAuthorDto> getAuthors(int page, int limit, String search) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        Page<BlogAuthor> authorPage = blogAuthorRepository.findAllFiltered(search, pageRequest);
        return PaginatedResponse.of(authorPage, blogMapper::toDto);
    }

    @Override
    public List<BlogAuthorDto> getAllAuthors() {
        return blogAuthorRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(blogMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BlogAuthorDto createAuthor(BlogAuthorDto dto) {
        BlogAuthor author = blogMapper.toEntity(dto);
        if (dto.getImageId() != null) {
            Media img = mediaRepository.findById(dto.getImageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Media", dto.getImageId()));
            author.setImage(img);
        }
        BlogAuthor saved = blogAuthorRepository.save(author);
        revalidationService.revalidate("blog_author", saved.getName(), "create");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public BlogAuthorDto updateAuthor(UUID id, BlogAuthorDto dto) {
        BlogAuthor author = blogAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));

        author.setName(dto.getName());
        author.setBio(dto.getBio());
        author.setDesignation(dto.getDesignation());

        if (dto.getImageId() != null) {
            Media img = mediaRepository.findById(dto.getImageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Media", dto.getImageId()));
            author.setImage(img);
        } else {
            author.setImage(null);
        }

        BlogAuthor saved = blogAuthorRepository.save(author);
        revalidationService.revalidate("blog_author", saved.getName(), "update");
        return blogMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteAuthor(UUID id) {
        BlogAuthor author = blogAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));

        // Find primary active author to reassign blogs to (or clear if none)
        BlogAuthor fallbackAuthor = blogAuthorRepository.findAll().stream()
                .filter(a -> !a.getId().equals(id) && !a.isDeleted())
                .findFirst()
                .orElse(null);

        // Reassign any blogs referencing this author to avoid EntityNotFoundException
        List<Blog> blogs = blogRepository.findAll().stream()
                .filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(id))
                .toList();

        for (Blog b : blogs) {
            b.setAuthor(fallbackAuthor);
            blogRepository.save(b);
        }

        author.setDeleted(true);
        author.setDeletedAt(Instant.now());
        blogAuthorRepository.save(author);
        revalidationService.revalidate("blog_author", author.getName(), "delete");
    }
}
