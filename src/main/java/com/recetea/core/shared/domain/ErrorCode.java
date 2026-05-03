package com.recetea.core.shared.domain;

/**
 * Stable identifiers for domain-layer error categories. The {@link DomainException}
 * base requires every concrete subclass to declare one of these codes, so the UI
 * adapter can dispatch on the value rather than on a long {@code instanceof} chain.
 *
 * Intentionally framework-free: no JavaFX, no HTTP-status coupling. The mapping
 * to alert severity (WARNING vs ERROR) lives in {@code GlobalExceptionHandler},
 * keeping {@code core.shared.domain} on the inside of the hexagon.
 */
public enum ErrorCode {

    /** Multi-field DTO validation failure (e.g. {@code SaveRecipeRequest.validate()}) or single-rule domain invariant. */
    VALIDATION_ERROR,

    /** Aggregate or entity not present (e.g. recipe id resolves to nothing in the repository). */
    NOT_FOUND,

    /** Caller is authenticated but lacks permission for this resource. */
    UNAUTHORIZED,

    /** No active session — the operation requires the caller to be logged in. */
    AUTHENTICATION_REQUIRED,

    /** Operation conflicts with current state (duplicate identity, double-vote, etc.). */
    CONFLICT,

    /** Catch-all for domain failures that don't fit a more specific category. */
    INTERNAL_ERROR
}
