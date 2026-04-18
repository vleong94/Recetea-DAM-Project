package com.recetea.core.recipe.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import com.recetea.core.user.domain.UserId;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Value Objects — Validación Fail-Fast")
class ValueObjectTest {

    @ParameterizedTest(name = "RecipeId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void recipeId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new RecipeId(value));
    }

    @Test
    void recipeId_acceptPositive() {
        assertDoesNotThrow(() -> new RecipeId(1));
    }

    @ParameterizedTest(name = "UserId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void userId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new UserId(value));
    }

    @ParameterizedTest(name = "CategoryId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void categoryId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new CategoryId(value));
    }

    @ParameterizedTest(name = "DifficultyId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void difficultyId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new DifficultyId(value));
    }

    @ParameterizedTest(name = "IngredientId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void ingredientId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new IngredientId(value));
    }

    @ParameterizedTest(name = "UnitId({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void unitId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new UnitId(value));
    }

    @ParameterizedTest(name = "PreparationTime({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -60})
    void preparationTime_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new PreparationTime(value));
    }

    @Test
    void preparationTime_acceptPositive() {
        assertDoesNotThrow(() -> new PreparationTime(30));
    }

    @ParameterizedTest(name = "Servings({0}) debe lanzar IllegalArgumentException")
    @ValueSource(ints = {0, -1, -10})
    void servings_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new Servings(value));
    }

    @Test
    void servings_acceptPositive() {
        assertDoesNotThrow(() -> new Servings(4));
    }
}
