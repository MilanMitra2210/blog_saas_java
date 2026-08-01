package com.quillforge.api.user.repository;

import com.quillforge.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM users u WHERE u.email = :email AND u.deleted = true LIMIT 1", nativeQuery = true)
    Optional<User> findDeletedByEmail(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM users u WHERE u.email = :email AND u.deleted = false", nativeQuery = true)
    boolean existsActiveByEmail(@Param("email") String email);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") UUID id);

    @Query("SELECT u FROM User u WHERE " +
           "( :search IS NULL OR :search = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ) AND " +
           "( :role IS NULL OR u.role = :role ) AND " +
           "( :status IS NULL OR :status = '' OR " +
           "  ( :status = 'active' AND u.active = true AND u.blocked = false ) OR " +
           "  ( :status = 'inactive' AND u.active = false ) OR " +
           "  ( :status = 'blocked' AND u.blocked = true ) )")
    Page<User> findAllFiltered(
            @Param("search") String search,
            @Param("role") User.RoleEnum role,
            @Param("status") String status,
            Pageable pageable);
}
