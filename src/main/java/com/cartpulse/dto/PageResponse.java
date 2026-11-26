package com.cartpulse.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination DTO demonstrating Java Generics and stable JSON serialization.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public static <T> PageResponse<T> from(Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.getNumber(),
                pageData.getSize()
        );
    }
}
