package com.quillforge.api.enquiry.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.enquiry.dto.EnquiryCreateDto;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;
import com.quillforge.api.enquiry.dto.EnquiryUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface EnquiryService {

    EnquiryResponseDto createEnquiry(EnquiryCreateDto dto, MultipartFile file);

    PaginatedResponse<EnquiryResponseDto> getAllEnquiries(
            int page,
            int limit,
            String search,
            String status,
            String type
    );

    EnquiryResponseDto getEnquiryById(UUID id);

    EnquiryResponseDto updateEnquiry(UUID id, EnquiryUpdateDto dto);

    void deleteEnquiry(UUID id);
}
