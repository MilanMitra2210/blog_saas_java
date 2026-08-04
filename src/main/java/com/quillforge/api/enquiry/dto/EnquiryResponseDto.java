package com.quillforge.api.enquiry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.media.dto.MediaDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class EnquiryResponseDto {
    private UUID id;
    private String type;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private String status;

    @JsonProperty("adminNotes")
    private String adminNotes;

    private MediaDto attachment;
    private String details;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;
}
