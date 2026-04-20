package com.recetea.core.shared.domain;

public record PageRequest(int page, int size) {

    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size < 1) throw new IllegalArgumentException("size must be >= 1");
    }

    public int offset() { return page * size; }
}
