package com.recetea.core.user.domain.vo;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    public Email {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El email es obligatorio.");
        if (!EMAIL_PATTERN.matcher(value.trim()).matches())
            throw new IllegalArgumentException("El email no tiene un formato válido: " + value);
    }
}
