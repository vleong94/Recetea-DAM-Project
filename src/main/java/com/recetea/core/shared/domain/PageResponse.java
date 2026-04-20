package com.recetea.core.shared.domain;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new PageResponse<>(content, totalElements, totalPages);
    }
}
