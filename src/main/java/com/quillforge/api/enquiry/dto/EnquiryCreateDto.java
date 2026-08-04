package com.quillforge.api.enquiry.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EnquiryCreateDto {
    private String type;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private UUID attachmentId;
    private String details;
}
