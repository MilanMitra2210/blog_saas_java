package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogCommentCreateDto;
import com.quillforge.api.blog.dto.BlogCommentResponseDto;
import com.quillforge.api.blog.entity.Blog;
import com.quillforge.api.blog.entity.BlogComment;
import com.quillforge.api.blog.repository.BlogCommentRepository;
import com.quillforge.api.blog.repository.BlogRepository;
import com.quillforge.api.auth.security.JwtAuthenticationFilter;
import com.quillforge.api.auth.service.TokenService;
import com.quillforge.api.user.repository.UserRepository;
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

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentRepository blogCommentRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    private static final List<String> SPAM_KEYWORDS = Arrays.asList(
            "free crypto", "bitcoin profit", "make money fast", "work from home online",
            "casino online", "slot machine", "buy cheap viagra", "cheap replica",
            "cialis online", "unsecured loan", "guaranteed wealth", "visit my site"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>\"]+|www\\.[^\\s<>\"]+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    @Transactional
    public BlogCommentResponseDto createComment(BlogCommentCreateDto dto, String authHeader) {
        if (isSpam(dto.getContent())) {
            throw new BadRequestException("Comment blocked: Spam signature detected.");
        }

        Blog post = blogRepository.findById(dto.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found"));

        boolean isUserAdmin = checkIsAdminUser(authHeader);

        BlogComment comment = new BlogComment();
        comment.setPostId(dto.getPostId());
        comment.setAuthorName(dto.getAuthorName());
        comment.setAuthorEmail(dto.getAuthorEmail());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setApproved(isUserAdmin); // Auto-approve if posted by an admin/editor

        BlogComment saved = blogCommentRepository.save(comment);
        return mapCommentToResponse(saved);
    }

    @Override
    public List<BlogCommentResponseDto> getApprovedCommentsForPost(UUID postId) {
        List<BlogComment> comments = blogCommentRepository.findByPostIdAndApprovedTrueAndParentIdIsNullOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::mapCommentToResponse).toList();
    }

    @Override
    public PaginatedResponse<BlogCommentResponseDto> getAdminComments(
            int page,
            int limit,
            UUID postId,
            Boolean approved,
            String search
    ) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<BlogComment> commentPage;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        String cleanSearch = hasSearch ? search.trim() : null;

        if (postId != null) {
            if (approved != null) {
                commentPage = hasSearch 
                        ? blogCommentRepository.searchRootThreadsByPostIdAndApproved(postId, approved, cleanSearch, pageRequest)
                        : blogCommentRepository.findThreadsByPostIdAndApproved(postId, approved, pageRequest);
            } else {
                commentPage = hasSearch
                        ? blogCommentRepository.searchRootThreadsByPostId(postId, cleanSearch, pageRequest)
                        : blogCommentRepository.findThreadsByPostIdAndApproved(postId, true, pageRequest); // fallback fallback root
            }
        } else {
            if (approved != null) {
                commentPage = hasSearch
                        ? blogCommentRepository.searchRootThreadsByApproved(approved, cleanSearch, pageRequest)
                        : blogCommentRepository.findRootThreadsByApproved(approved, pageRequest);
            } else {
                commentPage = hasSearch
                        ? blogCommentRepository.searchRootThreads(cleanSearch, pageRequest)
                        : blogCommentRepository.findRootThreads(pageRequest);
            }
        }

        return PaginatedResponse.of(commentPage, this::mapCommentToResponse);
    }

    @Override
    @Transactional
    public void approveComment(UUID commentId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setApproved(true);
        blogCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setDeleted(true);
        comment.setDeletedAt(java.time.Instant.now());
        blogCommentRepository.save(comment);
    }

    // --- Helpers ---

    private boolean isSpam(String content) {
        if (content == null) return false;
        String contentLower = content.toLowerCase();
        for (String kw : SPAM_KEYWORDS) {
            if (contentLower.contains(kw)) {
                return true;
            }
        }

        java.util.regex.Matcher m = URL_PATTERN.matcher(content);
        int linkCount = 0;
        while (m.find()) {
            linkCount++;
            if (linkCount > 2) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIsAdminUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            String token = authHeader.substring(7);
            if (tokenService.validateToken(token)) {
                return tokenService.getClaimsFromToken(token)
                        .map(claims -> {
                            String role = claims.get("role", String.class);
                            return "ADMIN".equalsIgnoreCase(role) || "EDITOR".equalsIgnoreCase(role);
                        }).orElse(false);
            }
        } catch (Exception e) {
            log.debug("Failed optional admin token resolve: {}", e.getMessage());
        }
        return false;
    }

    private BlogCommentResponseDto mapCommentToResponse(BlogComment c) {
        BlogCommentResponseDto res = new BlogCommentResponseDto();
        res.setId(c.getId());
        res.setPostId(c.getPostId());
        if (c.getPost() != null) {
            res.setPostTitle(c.getPost().getTitle());
            res.setPostSlug(c.getPost().getSlug());
        }
        res.setAuthorName(c.getAuthorName());
        res.setAuthorEmail(c.getAuthorEmail());
        res.setContent(c.getContent());
        res.setApproved(c.isApproved());
        res.setParentId(c.getParentId());
        res.setCreatedAt(c.getCreatedAt());

        // Check if author is registered Admin/Editor
        res.setAdmin(userRepository.findByEmail(c.getAuthorEmail())
                .map(u -> {
                    String roleName = u.getRole() != null ? u.getRole().name() : "";
                    return "ADMIN".equalsIgnoreCase(roleName) || "EDITOR".equalsIgnoreCase(roleName);
                }).orElse(false));

        if (c.getReplies() != null && !c.getReplies().isEmpty()) {
            res.setReplies(c.getReplies().stream()
                    .filter(r -> !r.isDeleted())
                    .map(this::mapCommentToResponse)
                    .toList());
        } else {
            res.setReplies(new ArrayList<>());
        }

        return res;
    }
}
