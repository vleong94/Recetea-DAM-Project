package com.recetea.core.shared.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Applicative validation container: accumulates all errors without short-circuiting.
 * Use Valid for success, Invalid for one or more failures.
 *
 * <pre>{@code
 * ValidationResult<Void> result =
 *     Validation.validate(title != null, "Title required")
 *               .and(Validation.validate(steps > 0, "At least one step"));
 *
 * result.getOrThrow(InvalidRecipeDataException::new);
 * }</pre>
 */
public sealed interface ValidationResult<T> permits ValidationResult.Valid, ValidationResult.Invalid {

    record Valid<T>(T value) implements ValidationResult<T> {
        @Override public boolean isValid() { return true; }
        @Override public List<String> errors() { return List.of(); }
    }

    record Invalid<T>(List<String> errors) implements ValidationResult<T> {
        public Invalid {
            if (errors == null || errors.isEmpty())
                throw new IllegalArgumentException("Invalid must carry at least one error message");
            errors = List.copyOf(errors);
        }
        @Override public boolean isValid() { return false; }
    }

    boolean isValid();
    List<String> errors();

    // --- Static factories ---

    static <T> Valid<T> valid(T value) {
        return new Valid<>(value);
    }

    static <T> Invalid<T> invalid(String error) {
        return new Invalid<>(List.of(error));
    }

    static <T> Invalid<T> invalid(List<String> errors) {
        return new Invalid<>(errors);
    }

    static <T> ValidationResult<T> of(T value, Predicate<T> condition, String errorMessage) {
        return condition.test(value) ? valid(value) : invalid(errorMessage);
    }

    static ValidationResult<Void> check(boolean condition, String errorMessage) {
        return condition ? valid(null) : invalid(errorMessage);
    }

    // --- Combinators ---

    /**
     * Merges errors from {@code other} into this result without short-circuiting.
     * If both are valid the original value is preserved; otherwise an Invalid with
     * all accumulated errors is returned.
     */
    default ValidationResult<T> and(ValidationResult<?> other) {
        if (isValid() && other.isValid()) return this;
        var combined = new ArrayList<>(errors());
        combined.addAll(other.errors());
        return invalid(combined);
    }

    /**
     * Aggregates errors from every supplied result in one pass.
     * Returns {@code Valid<Void>} only when every input is valid.
     */
    @SafeVarargs
    static ValidationResult<Void> combine(ValidationResult<?>... results) {
        var allErrors = Arrays.stream(results)
                .filter(r -> !r.isValid())
                .flatMap(r -> r.errors().stream())
                .toList();
        return allErrors.isEmpty() ? valid(null) : invalid(allErrors);
    }

    // --- Terminal operations ---

    /**
     * Returns the wrapped value on success, otherwise throws the exception
     * produced by {@code exceptionMapper} applied to the accumulated error list.
     */
    default T getOrThrow(Function<List<String>, ? extends RuntimeException> exceptionMapper) {
        if (this instanceof Valid<T> v) return v.value();
        throw exceptionMapper.apply(errors());
    }

    /** Maps the wrapped value if this result is valid; invalid results pass through unchanged. */
    default <U> ValidationResult<U> map(Function<T, U> mapper) {
        if (this instanceof Valid<T> v) return valid(mapper.apply(v.value()));
        return invalid(errors());
    }
}
