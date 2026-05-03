package com.recetea.core.user.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.shared.domain.ValidationResult;

import java.util.List;

/**
 * User-domain twin of {@code InvalidRecipeDataException} — accumulates
 * registration / login validation failures and reports them through
 * {@link ErrorCode#VALIDATION_ERROR}. Same multi-error pattern: the form
 * UI renders every failing field rather than only the first.
 *
 * <p><b>ES — </b>Gemela en el dominio de usuario de
 * {@code InvalidRecipeDataException} — acumula los fallos de validación
 * de registro / login y los reporta a través de
 * {@link ErrorCode#VALIDATION_ERROR}. Mismo patrón multi-error: la UI del
 * formulario renderiza todos los campos fallidos en lugar de sólo el
 * primero.
 */
public class InvalidUserDataException extends DomainException {

    private final List<String> errors;

    public InvalidUserDataException(List<String> errors) {
        super(ErrorCode.VALIDATION_ERROR, formatMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public static InvalidUserDataException from(ValidationResult<?> result) {
        return new InvalidUserDataException(result.errors());
    }

    public List<String> getErrors() {
        return errors;
    }

    /** Polymorphic override for the multi-error accumulator pattern. */
    @Override
    public List<String> errors() {
        return errors;
    }

    private static String formatMessage(List<String> errors) {
        return errors.size() + " validation error(s): " + String.join("; ", errors);
    }
}
