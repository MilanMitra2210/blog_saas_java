package com.quillforge.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " with id " + id + " not found", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(resourceName + " with " + fieldName + " '" + fieldValue + "' not found", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
