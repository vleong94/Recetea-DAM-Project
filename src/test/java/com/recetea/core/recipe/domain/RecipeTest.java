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
    @DisplayName("Debe actualizar las métricas internas al añadir valoraciones")
    void shouldUpdateInternalMetricsWhenRatingIsAdded() {
        Recipe recipe = createBaseRecipe();

        recipe.addRating(new UserId(2), new Score(5), "Perfecta");
        recipe.addRating(new UserId(3), new Score(4), "Muy buena");
        recipe.addRating(new UserId(4), new Score(3), "Correcta");

        assertEquals(3, recipe.getTotalRatings());
        assertEquals(0, BigDecimal.valueOf(4.00).setScale(2).compareTo(recipe.getAverageScore()));
    }

    @Test
    @DisplayName("Las métricas sociales deben conservarse tras sincronizar los pasos")
    void shouldMaintainMetricsAfterSyncSteps() {
        Recipe recipe = createBaseRecipe();
        recipe.addRating(new UserId(2), new Score(5), "Perfecta");

        BigDecimal scoreBefore = recipe.getAverageScore();
        int totalBefore = recipe.getTotalRatings();

        recipe.syncSteps(List.of(
                new RecipeStep(1, "Paso nuevo"),
                new RecipeStep(2, "Otro paso")
        ));

        assertEquals(totalBefore, recipe.getTotalRatings());
        assertEquals(0, scoreBefore.compareTo(recipe.getAverageScore()));
    }

    @Test
    @DisplayName("averageScore debe redondear correctamente a 2 decimales con HALF_UP")
    void shouldRoundAverageScoreToTwoDecimalPlaces() {
        Recipe recipe = createBaseRecipe();

        // 5 + 5 + 4 = 14 / 3 = 4.6666... → HALF_UP → 4.67
        recipe.addRating(new UserId(2), new Score(5), "Excelente");
        recipe.addRating(new UserId(3), new Score(5), "Perfecta");
        recipe.addRating(new UserId(4), new Score(4), "Muy buena");

        assertEquals(3, recipe.getTotalRatings());
        assertEquals(0, new BigDecimal("4.67").compareTo(recipe.getAverageScore()),
                "14/3 redondeado a 2 decimales con HALF_UP debe ser 4.67");
        assertEquals(2, recipe.getAverageScore().scale(),
                "El campo averageScore debe tener siempre escala 2 para coherencia con el esquema DB");
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
