package com.quillforge.api.cms.repository;

import com.quillforge.api.cms.entity.CMSPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CMSPageRepository extends JpaRepository<CMSPage, UUID> {

    Optional<CMSPage> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT p FROM CMSPage p WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<CMSPage> findAllFiltered(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
