package com.recetea.core.user.domain.vo;

public record Username(String value) {
    public Username {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        if (value.trim().length() < 3)
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres.");
    }
}
