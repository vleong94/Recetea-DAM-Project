package com.recetea.core.recipe.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.shared.domain.ValidationResult;

import java.util.List;

/**
 * Thrown when {@code SaveRecipeRequest.validate()} or one of the
 * {@code Recipe.syncXxx()} guards detects one or more rule violations.
 * Carries the full failure list, not just the first message, so the form
 * UI can render every error at once.
 *
 * <p>Maps to {@link ErrorCode#VALIDATION_ERROR} → WARNING-level dialog.
 * The {@code formatMessage} helper joins the failures into a single
 * sentence for log lines and the dialog body; the {@link #errors()}
 * accessor exposes the raw list for clients that need to render
 * field-level highlights.
 *
 * <p><b>ES — </b>Se lanza cuando {@code SaveRecipeRequest.validate()} o
 * uno de los guardas {@code Recipe.syncXxx()} detecta una o más
 * violaciones de regla. Lleva la lista completa de fallos, no sólo el
 * primer mensaje, para que la UI del formulario pueda renderizar todos
 * los errores a la vez.
 *
 * <p>Mapea a {@link ErrorCode#VALIDATION_ERROR} → diálogo de nivel
 * WARNING. El helper {@code formatMessage} une los fallos en una única
 * frase para las líneas de log y el cuerpo del diálogo; el accesor
 * {@link #errors()} expone la lista cruda para clientes que necesitan
 * renderizar resaltes a nivel de campo.
 */
public class InvalidRecipeDataException extends DomainException {

    private final List<String> errors;

    public InvalidRecipeDataException(List<String> errors) {
        super(ErrorCode.VALIDATION_ERROR, formatMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /** Convenience factory: wraps an {@link ValidationResult.Invalid} directly. */
    public static InvalidRecipeDataException from(ValidationResult<?> result) {
        return new InvalidRecipeDataException(result.errors());
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
