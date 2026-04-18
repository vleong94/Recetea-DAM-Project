package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ScoreTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Debe aceptar valores en rango [1-5]")
    void shouldAcceptValidRange(int value) {
        assertDoesNotThrow(() -> new Score(value));
        assertEquals(value, new Score(value).value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    @DisplayName("Debe rechazar valores fuera de rango [1-5]")
    void shouldRejectOutOfRange(int value) {
        assertThrows(IllegalArgumentException.class, () -> new Score(value));
    }

    @Test
    @DisplayName("Debe preservar el valor exacto")
    void shouldPreserveValue() {
        assertEquals(3, new Score(3).value());
    }
}
