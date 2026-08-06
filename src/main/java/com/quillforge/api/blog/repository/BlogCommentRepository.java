package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BlogCommentRepository extends JpaRepository<BlogComment, UUID> {

    List<BlogComment> findByPostIdAndApprovedTrueAndParentIdIsNullOrderByCreatedAtAsc(UUID postId);

    long countByPostIdAndApprovedTrue(UUID postId);

    @Query("SELECT c FROM BlogComment c WHERE c.postId = :postId AND c.approved = :approved AND c.parentId IS NULL")
    Page<BlogComment> findThreadsByPostIdAndApproved(
            @Param("postId") UUID postId,
            @Param("approved") boolean approved,
            Pageable pageable
    );

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL")
    Page<BlogComment> findRootThreads(Pageable pageable);

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL AND c.approved = :approved")
    Page<BlogComment> findRootThreadsByApproved(@Param("approved") boolean approved, Pageable pageable);

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL AND (" +
           "lower(c.content) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorName) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorEmail) LIKE lower(concat('%', :search, '%'))" +
           ")")
    Page<BlogComment> searchRootThreads(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL AND c.approved = :approved AND (" +
           "lower(c.content) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorName) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorEmail) LIKE lower(concat('%', :search, '%'))" +
           ")")
    Page<BlogComment> searchRootThreadsByApproved(
            @Param("approved") boolean approved,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL AND c.postId = :postId AND (" +
           "lower(c.content) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorName) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorEmail) LIKE lower(concat('%', :search, '%'))" +
           ")")
    Page<BlogComment> searchRootThreadsByPostId(
            @Param("postId") UUID postId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT c FROM BlogComment c WHERE c.parentId IS NULL AND c.postId = :postId AND c.approved = :approved AND (" +
           "lower(c.content) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorName) LIKE lower(concat('%', :search, '%')) OR " +
           "lower(c.authorEmail) LIKE lower(concat('%', :search, '%'))" +
           ")")
    Page<BlogComment> searchRootThreadsByPostIdAndApproved(
            @Param("postId") UUID postId,
            @Param("approved") boolean approved,
            @Param("search") String search,
            Pageable pageable
    );
}
