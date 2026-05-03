package com.recetea.core.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * Base for every recoverable domain-layer failure. Every subclass declares an
 * {@link ErrorCode} at construction, so the UI adapter can dispatch on the code
 * (a stable value) rather than on the concrete exception type (a brittle one).
 *
 * Multi-error subclasses ({@code InvalidRecipeDataException}, {@code InvalidUserDataException})
 * override {@link #errors()} to expose their accumulated list; single-rule
 * subclasses inherit the default — a one-element list containing
 * {@link #getMessage()}.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Polymorphic accessor for the human-readable failure list. Default is a
     * single-element list of the exception message; multi-error subclasses
     * override to return their accumulated validation failures.
     */
    public List<String> errors() {
        return List.of(getMessage() != null ? getMessage() : "");
    }
}
