package com.recetea.core.recipe.domain.vo;

import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Value Objects — Fail-Fast Validation")
class ValueObjectTest {

    // -------------------------------------------------------------------------
    // RecipeId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "RecipeId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void recipeId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new RecipeId(value));
    }

    @Test
    void recipeId_acceptPositive() {
        assertDoesNotThrow(() -> new RecipeId(1));
    }

    // -------------------------------------------------------------------------
    // UserId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "UserId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void userId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new UserId(value));
    }

    // -------------------------------------------------------------------------
    // CategoryId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "CategoryId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void categoryId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new CategoryId(value));
    }

    // -------------------------------------------------------------------------
    // DifficultyId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "DifficultyId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void difficultyId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new DifficultyId(value));
    }

    // -------------------------------------------------------------------------
    // IngredientId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "IngredientId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void ingredientId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new IngredientId(value));
    }

    // -------------------------------------------------------------------------
    // UnitId
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "UnitId({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -999})
    void unitId_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new UnitId(value));
    }

    // -------------------------------------------------------------------------
    // PreparationTime
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "PreparationTime({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -60})
    void preparationTime_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new PreparationTime(value));
    }

    @ParameterizedTest(name = "PreparationTime({0}) is a valid value")
    @ValueSource(ints = {1, 60, 1440, 43200})
    void preparationTime_acceptValidRange(int value) {
        assertDoesNotThrow(() -> new PreparationTime(value));
        assertEquals(value, new PreparationTime(value).value());
    }

    @Test
    @DisplayName("PreparationTime rejects just above the upper limit (43201)")
    void preparationTime_rejectJustAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new PreparationTime(43201));
    }

    @Test
    @DisplayName("PreparationTime rejects Integer.MAX_VALUE")
    void preparationTime_rejectExtremeValue() {
        assertThrows(IllegalArgumentException.class, () -> new PreparationTime(Integer.MAX_VALUE));
    }

    // -------------------------------------------------------------------------
    // Servings
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Servings({0}) should throw IllegalArgumentException")
    @ValueSource(ints = {0, -1, -10})
    void servings_rejectNonPositive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new Servings(value));
    }

    @ParameterizedTest(name = "Servings({0}) is a valid value")
    @ValueSource(ints = {1, 2, 100, 1000})
    void servings_acceptValidRange(int value) {
        assertDoesNotThrow(() -> new Servings(value));
        assertEquals(value, new Servings(value).value());
    }

    @Test
    @DisplayName("Servings rejects just above the upper limit (1001)")
    void servings_rejectJustAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new Servings(1001));
    }

    @Test
    @DisplayName("Servings rejects Integer.MAX_VALUE")
    void servings_rejectExtremeValue() {
        assertThrows(IllegalArgumentException.class, () -> new Servings(Integer.MAX_VALUE));
    }
}
