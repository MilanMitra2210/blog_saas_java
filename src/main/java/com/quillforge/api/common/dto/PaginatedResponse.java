package com.quillforge.api.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> items;
    private int page;
    private int limit;
    private long total;
    private int totalPages;

    public static <E, D> PaginatedResponse<D> of(
            org.springframework.data.domain.Page<E> springPage,
            java.util.function.Function<E, D> mapper) {

        return PaginatedResponse.<D>builder()
                .items(springPage.getContent().stream().map(mapper).toList())
                .page(springPage.getNumber() + 1)
                .limit(springPage.getSize())
                .total(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .build();
    }

    public static <T> PaginatedResponse<T> from(org.springframework.data.domain.Page<T> springPage) {
        return PaginatedResponse.<T>builder()
                .items(springPage.getContent())
                .page(springPage.getNumber() + 1)
                .limit(springPage.getSize())
                .total(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .build();
    }
}
