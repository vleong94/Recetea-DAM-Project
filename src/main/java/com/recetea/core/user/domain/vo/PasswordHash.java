package com.recetea.core.user.domain.vo;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El hash de contraseña es obligatorio.");
        if (!value.startsWith("$2"))
            throw new IllegalArgumentException("El hash de contraseña debe ser un hash BCrypt válido.");
    }
}
