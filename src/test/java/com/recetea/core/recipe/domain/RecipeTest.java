package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
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
    @DisplayName("Debe rechazar lista de ingredientes nula")
    void shouldRejectNullIngredients() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncIngredients(null));
    }

    @Test
    @DisplayName("Debe rechazar lista de ingredientes vacía")
    void shouldRejectEmptyIngredients() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncIngredients(Collections.emptyList()));
    }

    @Test
    @DisplayName("Debe rechazar lista de pasos nula")
    void shouldRejectNullSteps() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncSteps(null));
    }

    @Test
    @DisplayName("Debe rechazar lista de pasos vacía")
    void shouldRejectEmptySteps() {
        Recipe recipe = createBaseRecipe();
        assertThrows(Recipe.RecipeValidationException.class, () -> recipe.syncSteps(Collections.emptyList()));
    }

    @Test
    @DisplayName("Debe prohibir que el autor valore su propia receta")
    void shouldRejectSelfRating() {
        Recipe recipe = createBaseRecipe();
        UserId authorId = new UserId(1);
        assertThrows(Recipe.RecipeValidationException.class,
                () -> recipe.addRating(authorId, new Score(5), "Excelente"));
    }

    @Test
    @DisplayName("Debe permitir que otro usuario valore la receta")
    void shouldAllowRatingFromOtherUser() {
        Recipe recipe = createBaseRecipe();
        recipe.addRating(new UserId(2), new Score(4), "Muy buena");
        assertEquals(1, recipe.getRatings().size());
    }

    @Test
    @DisplayName("Debe rechazar una segunda valoración del mismo usuario")
    void shouldRejectDuplicateRatingFromSameUser() {
        Recipe recipe = createBaseRecipe();
        UserId voter = new UserId(2);
        recipe.addRating(voter, new Score(4), "Muy buena");
        assertThrows(Recipe.RecipeValidationException.class,
                () -> recipe.addRating(voter, new Score(3), "Intentando de nuevo"));
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
