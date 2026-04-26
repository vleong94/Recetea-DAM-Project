package com.recetea.core.recipe.domain;

import com.recetea.core.shared.domain.ValidationResult;

import java.util.List;

public class InvalidRecipeDataException extends RuntimeException {

    private final List<String> errors;

    public InvalidRecipeDataException(List<String> errors) {
        super(formatMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /** Convenience factory: wraps an {@link ValidationResult.Invalid} directly. */
    public static InvalidRecipeDataException from(ValidationResult<?> result) {
        return new InvalidRecipeDataException(result.errors());
    }

    public List<String> getErrors() {
        return errors;
    }

    private static String formatMessage(List<String> errors) {
        return errors.size() + " error(es) de validación: " + String.join("; ", errors);
    }
}
