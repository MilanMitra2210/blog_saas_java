package com.quillforge.api.enquiry.repository;

import com.quillforge.api.enquiry.entity.Enquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    @Query("SELECT e FROM Enquiry e WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.message) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR e.status = :status) AND " +
           "(:type IS NULL OR :type = '' OR e.type = :type)")
    Page<Enquiry> findAllFiltered(
            @Param("search") String search,
            @Param("status") String status,
            @Param("type") String type,
            Pageable pageable
    );

    long countByStatus(String status);
}
