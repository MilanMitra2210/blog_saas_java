package com.quillforge.api.enquiry.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.enquiry.dto.EnquiryCreateDto;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;
import com.quillforge.api.enquiry.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Public Enquiries", description = "Public endpoints for submitting contact and support enquiries")
public class PublicEnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping(value = "/enquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a contact or support enquiry")
    public ResponseEntity<ApiResponse<EnquiryResponseDto>> createEnquiry(
            @RequestParam("type") String type,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "details", required = false) String details,
            @RequestParam(value = "attachmentId", required = false) UUID attachmentId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        EnquiryCreateDto dto = new EnquiryCreateDto();
        dto.setType(type);
        dto.setName(name);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setSubject(subject);
        dto.setMessage(message);
        dto.setDetails(details);
        dto.setAttachmentId(attachmentId);

        EnquiryResponseDto response = enquiryService.createEnquiry(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enquiry submitted successfully", response));
    }
}
