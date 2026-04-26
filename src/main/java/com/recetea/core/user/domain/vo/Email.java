package com.recetea.core.user.domain.vo;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    public Email {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (!EMAIL_PATTERN.matcher(value.trim()).matches())
            throw new IllegalArgumentException("Invalid email format: " + value);
    }

    /** Shows only the domain part to prevent PII leakage in logs. */
    @Override
    public String toString() {
        int at = value.indexOf('@');
        String masked = at > 0 ? "***" + value.substring(at) : "***";
        return "Email[value=" + masked + "]";
    }
}
