package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.core.shared.domain.Validation;
import com.recetea.core.shared.domain.ValidationResult;

/**
 * Inbound command DTO for {@code IAddRatingUseCase}. Carries the rated
 * recipe, the score, and a required comment.
 *
 * <p>The user id is resolved from {@code IUserSessionService} inside the
 * use case (not carried in the DTO) so a crafted payload can't post a
 * rating as another user. The use case also enforces the
 * "no-self-rating" and "no-duplicate-vote" rules — neither belongs in
 * structural validation.
 *
 * <p><b>ES — </b>DTO de comando de entrada para
 * {@code IAddRatingUseCase}. Lleva la receta valorada, la puntuación y un
 * comentario obligatorio.
 *
 * <p>El id de usuario se resuelve desde {@code IUserSessionService}
 * dentro del caso de uso (no viaja en el DTO), de modo que un payload
 * manipulado no pueda publicar una valoración como otro usuario. El caso
 * de uso también aplica las reglas de "no auto-valoración" y "no voto
 * duplicado" — ninguna de las dos pertenece a la validación estructural.
 */
public record AddRatingRequest(
        RecipeId recipeId,
        Score score,
        String comment
) {

    /**
     * Non-short-circuit validation: every check is evaluated and any failures are
     * accumulated into a single {@link ValidationResult.Invalid} so the UI can show
     * the full list at once. The {@link Score} VO already enforces the [1-5] range
     * at construction, but the explicit check here gives clearer feedback than the
     * raw IllegalArgumentException that would otherwise surface on null input.
     */
    public ValidationResult<AddRatingRequest> validate() {
        return Validation.combine(
                Validation.validate(recipeId != null,
                        "Recipe ID is required."),
                Validation.validate(score != null,
                        "Score is required."),
                Validation.validate(score == null || (score.value() >= 1 && score.value() <= 5),
                        "Score must be between 1 and 5."),
                Validation.validate(comment != null,
                        "Comment is required."),
                Validation.validate(comment == null || comment.length() <= 1000,
                        "Comment must not exceed 1000 characters.")
        ).map(__ -> this);
    }
}
