package com.recetea.core.shared.domain;

import java.util.function.Predicate;

/** Fluent entry point for building {@link ValidationResult} chains. */
public final class Validation {

    private Validation() {}

    /** Validates a boolean condition; entry point for fluent chains. */
    public static ValidationResult<Void> validate(boolean condition, String errorMessage) {
        return ValidationResult.check(condition, errorMessage);
    }

    /** Validates a value against a predicate, capturing the value on success. */
    public static <T> ValidationResult<T> validate(T value, Predicate<T> condition, String errorMessage) {
        return ValidationResult.of(value, condition, errorMessage);
    }

    /** Aggregates errors from all supplied results without short-circuiting. */
    @SafeVarargs
    public static ValidationResult<Void> combine(ValidationResult<?>... results) {
        return ValidationResult.combine(results);
    }
}
