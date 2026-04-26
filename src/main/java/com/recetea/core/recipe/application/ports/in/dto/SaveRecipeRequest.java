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
    public record IngredientRequest(
            IngredientId ingredientId,
            UnitId unitId,
            BigDecimal quantity,
            String ingredientName,
            String unitName
    ) {}

    public record StepRequest(
            int stepOrder,
            String instruction
    ) {}

    public ValidationResult<Void> validate() {
        ValidationResult<Void> prepTime = preparationTimeMinutes <= 0
                ? ValidationResult.invalid("El tiempo de preparación debe ser mayor que cero.")
                : Validation.validate(preparationTimeMinutes <= PreparationTime.MAX_MINUTES,
                        "El tiempo de preparación no puede superar " + PreparationTime.MAX_MINUTES + " minutos (30 días).");

        ValidationResult<Void> servingsResult = servings <= 0
                ? ValidationResult.invalid("Las raciones deben ser mayores que cero.")
                : Validation.validate(servings <= Servings.MAX_SERVINGS,
                        "Las raciones no pueden superar " + Servings.MAX_SERVINGS + ".");

        return Validation.validate(title != null && !title.isBlank(), "El título es obligatorio.")
                .and(Validation.validate(description != null && !description.isBlank(), "La descripción es obligatoria."))
                .and(prepTime)
                .and(servingsResult)
                .and(validateIngredients())
                .and(validateSteps());
    }

    private ValidationResult<Void> validateIngredients() {
        if (ingredients == null || ingredients.isEmpty())
            return ValidationResult.invalid("La receta debe tener al menos un ingrediente.");
        ValidationResult<Void> result = ValidationResult.valid(null);
        for (int i = 0; i < ingredients.size(); i++) {
            var ir = ingredients.get(i);
            int pos = i + 1;
            ValidationResult<Void> check;
            if (ir == null)
                check = ValidationResult.invalid("El ingrediente en posición " + pos + " es nulo.");
            else if (ir.quantity() == null || ir.quantity().compareTo(BigDecimal.ZERO) <= 0)
                check = ValidationResult.invalid("El ingrediente en posición " + pos + " debe tener cantidad mayor que cero.");
            else
                check = ValidationResult.valid(null);
            result = result.and(check);
        }
        return result;
    }

    private ValidationResult<Void> validateSteps() {
        if (steps == null || steps.isEmpty())
            return ValidationResult.invalid("La receta debe tener al menos un paso.");
        ValidationResult<Void> result = ValidationResult.valid(null);
        for (var sr : steps) {
            if (sr == null || sr.instruction() == null || sr.instruction().isBlank())
                result = result.and(ValidationResult.invalid(
                        "El paso " + (sr != null ? sr.stepOrder() : "?") + " debe tener una instrucción."));
        }
        return result;
    }
}
