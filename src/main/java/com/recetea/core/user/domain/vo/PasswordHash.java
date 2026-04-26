package com.recetea.core.user.domain.vo;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Password hash is required.");
        if (!value.startsWith("$2"))
            throw new IllegalArgumentException("Password hash must be a valid BCrypt hash.");
    }

    /** Prevents the hash from appearing in logs or debug output. */
    @Override
    public String toString() {
        return "PasswordHash[value=PROTECTED]";
    }
}
