package com.quillforge.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String detail;
    private T data;

    public static <T> ApiResponse<T> success(String detail, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .detail(detail)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String detail) {
        return ApiResponse.<T>builder()
                .success(true)
                .detail(detail)
                .build();
    }

    public static <T> ApiResponse<T> error(String detail, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .detail(detail)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String detail) {
        return ApiResponse.<T>builder()
                .success(false)
                .detail(detail)
                .build();
    }
}
