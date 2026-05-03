package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.shared.domain.Validation;
import com.recetea.core.shared.domain.ValidationResult;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Inbound command DTO covering both the create and update flows for recipes —
 * {@code ICreateRecipeUseCase} and {@code IUpdateRecipeUseCase} accept the
 * same shape because they only differ in whether an existing aggregate is
 * loaded first. Carries primitives + ID-typed VOs for the parent fields and
 * nested {@link IngredientRequest} / {@link StepRequest} records for the two
 * child collections.
 *
 * <p><b>The {@code authorId} is intentionally absent.</b> Authorship is
 * sourced from {@code IUserSessionService} inside the use case, so a crafted
 * payload cannot impersonate another user. The same shape is reused for
 * updates with an additional ownership check inside {@code IUpdateRecipeUseCase}.
 *
 * <p><b>Validation strategy.</b> {@link #validate()} is non-short-circuit:
 * every check is evaluated and failures accumulate into a single
 * {@code ValidationResult.Invalid} so the form can render every error at
 * once. The duplicate-ingredient case emits an i18n key
 * ({@link #DUPLICATE_INGREDIENT_KEY}) rather than a literal message because
 * it carries enough nuance for UI copywriting; the rest are plain English
 * strings since they are predictable structural checks.
 *
 * <p>Range constants ({@link PreparationTime#MAX_MINUTES},
 * {@link Servings#MAX_SERVINGS}) are pulled from the value-object classes so
 * the DTO and the VO compact constructors can never disagree.
 *
 * <p><b>ES — </b>DTO de comando de entrada que cubre tanto el flujo de
 * creación como el de actualización de recetas — {@code ICreateRecipeUseCase}
 * e {@code IUpdateRecipeUseCase} aceptan la misma forma porque sólo
 * difieren en si se carga primero un agregado existente. Lleva primitivos
 * + VOs tipados por id para los campos del padre y records anidados
 * {@link IngredientRequest} / {@link StepRequest} para las dos colecciones
 * hijas.
 *
 * <p><b>El {@code authorId} está ausente intencionadamente.</b> La autoría
 * se obtiene de {@code IUserSessionService} dentro del caso de uso, de
 * modo que un payload manipulado no pueda suplantar a otro usuario. La
 * misma forma se reutiliza para las actualizaciones con una comprobación
 * adicional de propiedad dentro de {@code IUpdateRecipeUseCase}.
 *
 * <p><b>Estrategia de validación.</b> {@link #validate()} no
 * corto-circuita: cada comprobación se evalúa y los fallos se acumulan en
 * un único {@code ValidationResult.Invalid} para que el formulario pueda
 * renderizar todos los errores a la vez. El caso de ingrediente duplicado
 * emite una clave i18n ({@link #DUPLICATE_INGREDIENT_KEY}) en lugar de un
 * mensaje literal porque tiene suficiente matiz para la redacción UX; el
 * resto son cadenas en inglés ya que son comprobaciones estructurales
 * predecibles.
 *
 * <p>Las constantes de rango ({@link PreparationTime#MAX_MINUTES},
 * {@link Servings#MAX_SERVINGS}) se toman de las clases value object, de
 * modo que el DTO y los constructores compactos de los VOs no puedan
 * discrepar.
 */
public record SaveRecipeRequest(
        CategoryId categoryId,
        DifficultyId difficultyId,
        String title,
        String description,
        int preparationTimeMinutes,
        int servings,
        java.util.List<IngredientRequest> ingredients,
        java.util.List<StepRequest> steps
) {
    /**
     * Single ingredient line on a recipe. {@code ingredientName} and
     * {@code unitName} are display-only echoes for round-tripping in the UI
     * (so the table can show labels without re-querying the catalogue) and
     * are ignored on the persistence path — only the IDs and quantity are
     * authoritative.
     *
     * <p><b>ES — </b>Una única línea de ingrediente de una receta.
     * {@code ingredientName} y {@code unitName} son ecos sólo de
     * presentación para el ida-y-vuelta en la UI (para que la tabla pueda
     * mostrar etiquetas sin volver a consultar el catálogo) y se ignoran
     * en la ruta de persistencia — sólo los IDs y la cantidad son
     * autoritativos.
     */
    public record IngredientRequest(
            IngredientId ingredientId,
            UnitId unitId,
            BigDecimal quantity,
            String ingredientName,
            String unitName
    ) {}

    /**
     * Single recipe step. {@code stepOrder} is 1-based and must be unique
     * within a request — duplicate ordinals are rejected by
     * {@code Recipe.syncSteps()} during persistence.
     *
     * <p><b>ES — </b>Un único paso de receta. {@code stepOrder} es 1-based
     * y debe ser único dentro de una request — los ordinales duplicados
     * los rechaza {@code Recipe.syncSteps()} durante la persistencia.
     */
    public record StepRequest(
            int stepOrder,
            String instruction
    ) {}

    public ValidationResult<Void> validate() {
        ValidationResult<Void> prepTime = preparationTimeMinutes <= 0
                ? ValidationResult.invalid("Preparation time must be greater than zero.")
                : Validation.validate(preparationTimeMinutes <= PreparationTime.MAX_MINUTES,
                        "Preparation time must not exceed " + PreparationTime.MAX_MINUTES + " minutes (30 days).");

        ValidationResult<Void> servingsResult = servings <= 0
                ? ValidationResult.invalid("Servings must be greater than zero.")
                : Validation.validate(servings <= Servings.MAX_SERVINGS,
                        "Servings must not exceed " + Servings.MAX_SERVINGS + ".");

        return Validation.validate(title != null && !title.isBlank(), "Title is required.")
                .and(Validation.validate(description != null && !description.isBlank(), "Description is required."))
                .and(prepTime)
                .and(servingsResult)
                .and(validateIngredients())
                .and(validateSteps());
    }

    /** i18n key emitted by {@link #validateIngredients()} when the ingredient list contains a duplicate id. */
    public static final String DUPLICATE_INGREDIENT_KEY = "error.DUPLICATE_INGREDIENT";

    private ValidationResult<Void> validateIngredients() {
        if (ingredients == null || ingredients.isEmpty())
            return ValidationResult.invalid("Recipe must have at least one ingredient.");
        ValidationResult<Void> result = ValidationResult.valid(null);
        Set<IngredientId> seenIds = new HashSet<>();
        boolean duplicateReported = false;
        for (int i = 0; i < ingredients.size(); i++) {
            var ir = ingredients.get(i);
            int pos = i + 1;
            ValidationResult<Void> check;
            if (ir == null) {
                check = ValidationResult.invalid("Ingredient at position " + pos + " is null.");
            } else if (ir.quantity() == null || ir.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                check = ValidationResult.invalid("Ingredient at position " + pos + " must have a quantity greater than zero.");
            } else if (ir.ingredientId() != null && !seenIds.add(ir.ingredientId())) {
                if (duplicateReported) {
                    check = ValidationResult.valid(null);
                } else {
                    duplicateReported = true;
                    check = ValidationResult.invalid(DUPLICATE_INGREDIENT_KEY);
                }
            } else {
                check = ValidationResult.valid(null);
            }
            result = result.and(check);
        }
        return result;
    }

    private ValidationResult<Void> validateSteps() {
        if (steps == null || steps.isEmpty())
            return ValidationResult.invalid("Recipe must have at least one step.");
        ValidationResult<Void> result = ValidationResult.valid(null);
        for (var sr : steps) {
            if (sr == null || sr.instruction() == null || sr.instruction().isBlank())
                result = result.and(ValidationResult.invalid(
                        "Step " + (sr != null ? sr.stepOrder() : "?") + " must have an instruction."));
        }
        return result;
    }
}
