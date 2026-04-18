package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {

    private Recipe createBaseRecipe() {
        return new Recipe(
                new UserId(1),
                new Category(new CategoryId(1), "Entrantes"),
                new Difficulty(new DifficultyId(1), "Fácil"),
                "Receta de Prueba",
                "Descripción de prueba",
                new PreparationTime(20),
                new Servings(2)
        );
    }

    @Test
    @DisplayName("Debe detectar y prohibir órdenes de pasos duplicados")
    void shouldPreventDuplicateSteps() {
        Recipe recipe = createBaseRecipe();

        assertThrows(Recipe.RecipeValidationException.class, () ->
                recipe.syncSteps(List.of(
                        new RecipeStep(1, "Paso A"),
                        new RecipeStep(1, "Paso B")
                ))
        );
    }

    @Test
    @DisplayName("Debe permitir la adición de ingredientes y reflejar el conteo correcto")
    void shouldAddIngredients() {
        Recipe recipe = createBaseRecipe();
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100))
        ));

        assertEquals(1, recipe.getIngredients().size());
    }

    @Test
    @DisplayName("Debe lanzar excepción ante métricas de tiempo negativas")
    void shouldValidatePreparationTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new Recipe(
                        new UserId(1),
                        new Category(new CategoryId(1), "A"),
                        new Difficulty(new DifficultyId(1), "B"),
                        "T", "D",
                        new PreparationTime(-10),
                        new Servings(2)
                )
        );
    }
}
