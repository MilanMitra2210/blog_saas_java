package com.quillforge.api.media.repository;

import com.quillforge.api.media.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    @Query("SELECT m FROM Media m WHERE " +
            "(:folderId IS NULL OR m.folderId = :folderId) AND " +
            "(:search IS NULL OR :search = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Media> findAllFiltered(
            @Param("folderId") UUID folderId,
            @Param("search") String search,
            Pageable pageable
    );

    List<Media> findByFolderId(UUID folderId);
}
