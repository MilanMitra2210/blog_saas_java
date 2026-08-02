package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogSlugRedirect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogSlugRedirectRepository extends JpaRepository<BlogSlugRedirect, UUID> {
    Optional<BlogSlugRedirect> findByOldSlug(String oldSlug);
    void deleteByOldSlug(String oldSlug);
}
