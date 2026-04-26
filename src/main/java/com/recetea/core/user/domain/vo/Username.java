package com.recetea.core.user.domain.vo;

public record Username(String value) {
    public Username {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (value.trim().length() < 3)
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
    }
}
